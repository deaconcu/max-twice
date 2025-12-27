package com.prosper.learn.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.dto.DailyStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyMapper;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.common.constants.RedisStatsConstants.*;
import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 日常统计数据同步服务
 *
 * 该服务负责将Redis中的实时统计数据同步到数据库进行持久化存储，同时提供统计数据的查询功能。
 *
 * 设计思路:
 * 1. 实时统计数据存储在Redis中，性能高但非持久化
 * 2. 定期将Redis数据同步到数据库，确保数据不丢失
 * 3. 支持补偿机制，处理同步失败的情况
 * 4. 提供统一的用户和内容统计数据查询接口
 *
 * Redis数据结构:
 * - 用户统计: stats:YYYY-MM-DD:user -> {userId:statType: count}
 *   例如: stats:2025-12-06:user -> {"123:view": 10, "123:twice": 5}
 *
 * - 内容统计: stats:YYYY-MM-DD:content -> {contentType:contentId:statType: count}
 *   例如: stats:2025-12-06:content -> {"1:456:view": 100, "1:456:twice": 20}
 *
 * 数据库存储结构（JSON 数组格式，节省空间）:
 * - user_stats_yearly.stats -> {"1-1": [10, 5, 2, 7], "1-2": [15, 8, 3, 9], ...}
 *   数组顺序：[views, twice, like, comments]
 *
 * - content_stats_yearly.stats -> {"1-1": [100, 20, 15, 30], "1-2": [120, 25, 18, 35], ...}
 *   数组顺序：[views, twice, likes, comments]
 *
 * 数据库累计表（content_stats）:
 * - content_stats 表存储累计总数（不是明细）
 * - 用于快速查询总统计：总统计 = content_stats 累计 + 今日 Redis 增量
 *
 * 统计类型包括:
 * - view/views: 浏览量
 * - twice: 两次能懂点赞
 * - like/likes: 有用点赞
 * - comment/comments: 评论数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStatsService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ContentStatsYearlyMapper contentStatsYearlyMapper;
    private final UserStatsYearlyMapper userStatsYearlyMapper;
    private final ContentStatsDataService contentStatsDataService;
    private final UserStatsDataService userStatsDataService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;

    /**
     * 同步昨天的统计数据
     * 
     * 主要同步任务，通常在每日凌晨执行:
     * 1. 从Redis读取昨天的用户和文章统计数据
     * 2. 将数据写入数据库对应的统计表
     * 3. 删除Redis中已同步的数据以节省内存
     */
    public void syncYesterdayStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.toString();
        
        log.info("开始同步{}的统计数据", dateStr);
        
        try {
            // 同步用户统计
            int userStatsCount = syncUserStats(dateStr);
            
            // 同步文章统计
            int postStatsCount = syncPostStats(dateStr);
            
            log.info("同步{}的数据完成: 用户统计{}条, 文章统计{}条", dateStr, userStatsCount, postStatsCount);
            
        } catch (Exception e) {
            log.error("同步{}的数据失败", dateStr, e);
        }
    }

    /**
     * 同步指定日期的统计数据
     * 
     * 手动触发同步任务，用于重新处理指定日期的数据:
     * 1. 检查Redis中是否有指定日期的数据
     * 2. 如果有数据，则重新同步到数据库
     * 3. 如果没有数据，直接退出，不覆盖数据库中的现有数据
     * 
     * @param date 要同步的日期，如果为null则默认为今天
     * @return 同步结果信息
     */
    public String syncSpecificDate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        String dateStr = date.toString();
        log.info("开始同步{}的统计数据", dateStr);
        
        try {
            // 检查Redis中是否有这个日期的数据
            String userKey = generateUserStatsKey(dateStr);
            String postKey = generatePostStatsKey(dateStr);
            
            boolean hasUserData = redisTemplate.hasKey(userKey);
            boolean hasPostData = redisTemplate.hasKey(postKey);
            
            if (!hasUserData && !hasPostData) {
                String message = String.format("Redis中没有%s的统计数据，跳过同步", dateStr);
                log.info(message);
                return message;
            }
            
            int userStatsCount = 0;
            int postStatsCount = 0;
            
            if (hasUserData) {
                userStatsCount = syncUserStats(dateStr);
                log.info("同步{}的用户数据: {}条", dateStr, userStatsCount);
            }
            
            if (hasPostData) {
                postStatsCount = syncPostStats(dateStr);
                log.info("同步{}的文章数据: {}条", dateStr, postStatsCount);
            }
            
            String message = String.format("同步%s的数据完成: 用户统计%d条, 文章统计%d条", 
                dateStr, userStatsCount, postStatsCount);
            log.info(message);
            return message;
            
        } catch (Exception e) {
            String message = String.format("同步%s的数据失败: %s", dateStr, e.getMessage());
            log.error(message, e);
            return message;
        }
    }


    /**
     * 同步用户统计数据
     * 
     * 同步逻辑，直接覆盖当天的完整统计数据:
     * 1. 从Redis获取指定日期的用户统计数据
     * 2. 解析并聚合每个用户当天的完整数据
     * 3. 直接覆盖数据库中当天的数据（而非增量更新）
     * 4. 删除Redis中的原始数据
     * 
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    private int syncUserStats(String dateStr) {
        String userKey = generateUserStatsKey(dateStr);
        Map<Object, Object> userStats = redisTemplate.opsForHash().entries(userKey);
        
        if (userStats.isEmpty()) {
            log.info("{}没有用户统计数据", dateStr);
            return 0;
        }
        
        // 解析日期信息
        LocalDate date = LocalDate.parse(dateStr);
        int year = date.getYear();
        String dayKey = generateDayKey(date);
        
        // 按用户ID分组并聚合当天的完整统计数据
        Map<Long, UserDayStats> userDayStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : userStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (count <= 0) continue; // 跳过无效数据
            
            String[] parts = field.split(":");
            if (parts.length != 2) continue;
            
            Long userId = Long.parseLong(parts[0]);
            String statType = parts[1];
            
            UserDayStats dayStats = userDayStatsMap.computeIfAbsent(userId, k -> new UserDayStats());
            
            // 聚合当天的完整数据
            switch (statType) {
                case STAT_TYPE_VIEW:
                    dayStats.views += count;
                    break;
                case STAT_TYPE_TWICE:
                    dayStats.twice += count;
                    break;
                case STAT_TYPE_LIKE:
                    dayStats.like += count;
                    break;
                case STAT_TYPE_COMMENT:
                    dayStats.comments += count;
                    break;
                default:
                    log.debug("忽略未知的统计类型: {}", statType);
            }
        }
        
        // 直接覆盖数据库中当天的数据
        int updateCount = 0;
        for (Map.Entry<Long, UserDayStats> entry : userDayStatsMap.entrySet()) {
            Long userId = entry.getKey();
            UserDayStats dayStats = entry.getValue();

            try {
                // 确保用户的年度记录存在
                ensureUserYearRecord(userId, year);

                // 直接设置当天的完整数据（覆盖而非增量）
                int updated = userStatsYearlyMapper.updateYearlyStatsArray(
                        userId, year, dayKey,
                        dayStats.views, dayStats.twice, dayStats.like,
                        dayStats.comments);

                if (updated > 0) {
                    log.debug("覆盖用户{}在{}的统计数据: views={}, twice={}, like={}, comments={}",
                        userId, dateStr, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    // 同步更新用户总计表
                    userStatsDataService.increase(userId, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    updateCount++;
                } else {
                    log.warn("用户{}的{}年度记录不存在，无法更新统计", userId, year);
                }

            } catch (Exception e) {
                log.error("同步用户{}在{}的统计数据失败", userId, dateStr, e);
            }
        }
        
        // 删除Redis数据，释放内存空间（只有非当天的数据才删除）
        LocalDate today = LocalDate.now();
        LocalDate syncDate = LocalDate.parse(dateStr);
        if (!syncDate.equals(today)) {
            redisTemplate.delete(userKey);
            log.info("删除Redis中{}的用户统计数据", dateStr);
        } else {
            log.info("当天数据同步完成，保留Redis中{}的用户统计数据以继续收集", dateStr);
        }
        
        return updateCount;
    }
    
    /**
     * 用户单日统计数据结构
     */
    private static class UserDayStats {
        int views = 0;
        int twice = 0;
        int like = 0;
        int comments = 0;
    }
    
    /**
     * 文章单日统计数据结构
     */
    private static class PostDayStats {
        int views = 0;
        int twice = 0;
        int like = 0;
        int comments = 0;
    }
    
    /**
     * 确保用户的年度统计记录存在
     */
    private void ensureUserYearRecord(long userId, int year) {
        UserStatsYearlyDO existing = userStatsYearlyMapper.getByUserIdAndYear(userId, year);
        if (existing == null) {
            UserStatsYearlyDO yearRecord = new UserStatsYearlyDO();
            yearRecord.setUserId(userId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}"); // 初始化为空JSON对象
            userStatsYearlyMapper.insert(yearRecord);
            log.debug("创建用户{}的{}年度统计记录", userId, year);
        }
    }

    /**
     * 同步文章统计数据
     * 
     * 处理逻辑:
     * 1. 从Redis获取指定日期的文章统计数据
     * 2. 解析field格式: "postId:statType" -> count
     * 3. 按文章ID分组聚合各种统计类型的数据，同时进行字段名映射
     * 4. 更新post_stats表中的JSON字段
     * 5. 删除Redis中的原始数据
     * 
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    private int syncPostStats(String dateStr) {
        String postKey = generatePostStatsKey(dateStr);
        Map<Object, Object> postStats = redisTemplate.opsForHash().entries(postKey);
        
        if (postStats.isEmpty()) {
            log.info("{}没有文章统计数据", dateStr);
            return 0;
        }
        
        // 按文章ID分组，使用数据库字段名
        Map<Long, PostDayStats> postStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : postStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());

            if (count <= 0) continue;

            String[] parts = field.split(":");
            if (parts.length != 3) continue; // 现在格式是 contentType:contentId:statType

            // 解析新格式: contentType:contentId:statType
            String contentType = parts[0];
            Long postId = Long.parseLong(parts[1]);
            String redisStatType = parts[2];

            // 只处理 POST 类型的内容
            if (!String.valueOf(ContentType.post.value()).equals(contentType)) {
                continue;
            }
            
            PostDayStats dayStats = postStatsMap.computeIfAbsent(postId, k -> new PostDayStats());
            
            // 🔴 修复字段名映射：Redis使用单数，数据库使用复数
            switch (redisStatType) {
                case STAT_TYPE_VIEW:
                    dayStats.views += count;
                    break;
                case STAT_TYPE_TWICE:
                    dayStats.twice += count;
                    break;
                case STAT_TYPE_LIKE:
                    dayStats.like += count;
                    break;
                case STAT_TYPE_COMMENT:
                    dayStats.comments += count;
                    break;
                default:
                    log.debug("忽略未知的统计类型: {}", redisStatType);
            }
        }
        
        // 更新post_stats表，直接覆盖当天的完整数据
        int updateCount = 0;
        int year = LocalDate.parse(dateStr).getYear();
        String dayKey = dateStr.substring(5).replace("-", "-"); // "08-22"
        
        for (Map.Entry<Long, PostDayStats> entry : postStatsMap.entrySet()) {
            Long postId = entry.getKey();
            PostDayStats dayStats = entry.getValue();
            
            try {
                // 确保post_stats年度记录存在
                ensurePostYearRecord(ContentType.post.value(), postId, year);
                
                // 直接设置当天的完整数据（覆盖而非增量）
                int updated = contentStatsYearlyMapper.setDayStats(ContentType.post.value(), postId, year, dayKey,
                        dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);
                
                if (updated > 0) {
                    log.debug("覆盖文章{}在{}的统计数据: views={}, twice={}, like={}, comments={}",
                        postId, dateStr, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    // 同步更新内容总计表
                    contentStatsDataService.increase(ContentType.post, postId,
                        dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    updateCount++;
                } else {
                    log.warn("文章{}的{}年度记录不存在，无法更新统计", postId, year);
                }
                
            } catch (Exception e) {
                log.error("同步文章{}在{}的统计数据失败", postId, dateStr, e);
            }
        }
        
        // 同步成功后删除Redis数据（只有非当天的数据才删除）
        LocalDate today = LocalDate.now();
        LocalDate syncDate = LocalDate.parse(dateStr);
        if (!syncDate.equals(today)) {
            redisTemplate.delete(postKey);
            log.info("删除Redis中{}的文章统计数据", dateStr);
        } else {
            log.info("当天数据同步完成，保留Redis中{}的文章统计数据以继续收集", dateStr);
        }
        
        return updateCount;
    }

// --注释掉检查 START (2025/12/10 11:34):
//    /**
//     * 更新post_stats表中的字段
//     *
//     * 该方法负责将Redis中的统计数据写入数据库的post_stats表:
//     * 1. 确保年度记录存在
//     * 2. 使用PostStatsMapper进行原子性的数值设置
//     * 3. 支持直接覆盖，确保数据一致性
//     *
//     * @param type 对象类型，如"POST", "NODE", "ROADMAP"
//     * @param objectId 对象ID
//     * @param year 统计年份
//     * @param dayKey 日期键，格式如"8-22"
//     * @param field 统计字段，如"view", "twice", "like", "comment"
//     * @param count 统计数值
//     */
//    private void updatePostStatsField(byte type, Long objectId, int year, String dayKey, String field, int count) {
//        try {
//            // 确保post_stats年度记录存在
//            ensurePostYearRecord(type, objectId, year);
//
//            // 使用PostStatsMapper直接设置当天的统计值
//            Map<String, Integer> dayStats = getCurrentPostDayStats(type, objectId, year, dayKey);
//
//            // 🔴 字段名映射：Redis使用单数，数据库使用复数
//            String dbField = field;
//            switch (field) {
//                case STAT_TYPE_VIEW:
//                    dbField = "views";
//                    break;
//                case STAT_TYPE_COMMENT:
//                    dbField = "comments";
//                    break;
//                // twice 和 like 保持不变，不需要映射
//            }
//
//            dayStats.put(dbField, count);
//
//            int updated = contentStatsYearlyMapper.setDayStats(type, objectId, year, dayKey,
//                    dayStats.getOrDefault("views", 0),
//                    dayStats.getOrDefault("twice", 0),
//                    dayStats.getOrDefault("like", 0),
//                    dayStats.getOrDefault("comments", 0));
//
//            if (updated > 0) {
//                log.debug("设置post_stats: type={}, objectId={}, dayKey={}, field={}->{}, count={}",
//                    type, objectId, dayKey, field, dbField, count);
//            } else {
//                log.warn("post_stats记录不存在: type={}, objectId={}, year={}", type, objectId, year);
//            }
//
//        } catch (Exception e) {
//            log.error("更新post_stats失败: type={}, objectId={}, field={}, count={}",
//                type, objectId, field, count, e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:34)

    /**
     * 确保post_stats的年度记录存在
     */
    private void ensurePostYearRecord(int type, Long objectId, int year) {
        ContentStatsYearlyDO existing = contentStatsYearlyMapper.getByTypeAndObjectIdAndYear(type, objectId, year);
        if (existing == null) {
            ContentStatsYearlyDO yearRecord = new ContentStatsYearlyDO();
            yearRecord.setObjectType(type);
            yearRecord.setObjectId(objectId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}");
            contentStatsYearlyMapper.insert(yearRecord);
            log.debug("创建{}对象{}的{}年度统计记录", type, objectId, year);
        }
    }
    
    /**
     * 获取当前post指定日期的统计数据
     */
    private Map<String, Integer> getCurrentPostDayStats(byte type, Long objectId, int year, String dayKey) {
        try {
            String dayStatsJson = contentStatsYearlyMapper.getDayStats(type, objectId, year, dayKey);
            if (dayStatsJson != null) {
                return objectMapper.readValue(dayStatsJson, new TypeReference<Map<String, Integer>>() {});
            }
        } catch (Exception e) {
            log.debug("获取post当日统计失败，使用默认值: type={}, objectId={}, dayKey={}", type, objectId, dayKey);
        }
        
        Map<String, Integer> defaultStats = new HashMap<>();
        defaultStats.put("views", 0);
        defaultStats.put("twice", 0);
        defaultStats.put("like", 0);
        defaultStats.put("comments", 0);
        return defaultStats;
    }

    // ====== 查询服务方法 ======
    // 以下方法提供统计数据的查询功能，支持从Redis读取实时数据和从数据库读取历史数据

    /**
     * 获取用户今日统计（从Redis）
     * 
     * 读取当天的实时统计数据，数据来源是Redis:
     * 1. 构造今日的Redis key
     * 2. 获取该用户的所有统计数据
     * 3. 解析并聚合成UserStatsDTO对象
     * 
     * 缓存策略：按用户ID+日期缓存，避免跨日期数据污染
     */

    /*
    @Cacheable(value = "todayStats",
               key = "'user:' + #userId + ':' + T(java.time.LocalDate).now().toString()",
               unless = "#result == null")
     */
    public UserStatsDTO getUserTodayStats(long userId) {
        validateUserId(userId);
        String today = LocalDate.now().toString();
        String userKey = generateUserStatsKey(today);

        String uKey = generatePostStatsKey(today);
        Map<Object, Object> s2tats = redisTemplate.opsForHash().entries(uKey);

        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(userKey);
            return parseUserStatsFromRedis(userId, stats, today);
        } catch (Exception e) {
            log.error("获取用户{}今日统计失败", userId, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户昨日统计（从数据库）
     * 
     * 缓存策略：按用户ID+昨日日期缓存，确保不同日期的"昨日"数据独立
     */
    @Cacheable(value = "yesterdayStats", 
               key = "'user:' + #userId + ':' + T(java.time.LocalDate).now().minusDays(1).toString()", 
               unless = "#result == null")
    public UserStatsDTO getUserYesterdayStats(long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            Map<String, Integer> stats = getUserDayStats(userId, yesterday);
            
            UserStatsDTO dto = UserStatsDTO.builder()
                .userId(userId)
                .views(stats.get("views"))
                .twices(stats.get("twice"))
                .likes(stats.get("like"))
                .comments(stats.get("comments"))
                .build();
                
            return dto;
        } catch (Exception e) {
            log.error("获取用户{}昨日统计失败", userId, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户历史统计（从数据库）
     * 
     * 缓存策略：按用户ID+天数+结束日期缓存，确保不同时间窗口的历史数据独立
     */
    @Cacheable(value = "historyStats", 
               key = "'user:' + #userId + ':' + #days + ':' + T(java.time.LocalDate).now().minusDays(1).toString()", 
               unless = "#result == null")
    public UserStatsDTO getUserHistoryStats(long userId, int days) {
        validateUserId(userId);
        validateDaysRange(days);
        LocalDate endDate = LocalDate.now().minusDays(1); // 不包括今天
        LocalDate startDate = endDate.minusDays(days - 1);
        
        try {
            Map<String, Integer> totalStats = getUserDateRangeStats(userId, startDate, endDate);
            
            UserStatsDTO dto = UserStatsDTO.builder()
                .userId(userId)
                .views(totalStats.get("views"))
                .twices(totalStats.get("twice"))
                .likes(totalStats.get("like"))
                .comments(totalStats.get("comments"))
                .build();
                
            return dto;
        } catch (Exception e) {
            log.error("获取用户{}历史{}天统计失败", userId, days, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户时间段统计（包含每日明细）
     */
    public UserStatsDTO getUserPeriodStatsWithDaily(long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        try {
            List<DailyStatsDTO> dailyStats = new ArrayList<>();

            // 总计数据
            int totalViews = 0;
            int totalTwice = 0;
            int totalLikes = 0;
            int totalComments = 0;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                DailyStatsDTO dailyStat;
                
                if (date.equals(LocalDate.now())) {
                    // 今日数据从Redis获取
                    UserStatsDTO todayStats = getUserTodayStats(userId);
                    dailyStat = DailyStatsDTO.builder()
                        .date(date.toString())
                        .views(todayStats.getViews())
                        .twice(todayStats.getTwices())
                        .likes(todayStats.getLikes())
                        .comments(todayStats.getComments())
                        .build();
                } else {
                    // 历史数据从数据库获取
                    Map<String, Integer> dayStats = getUserDayStats(userId, date);

                    dailyStat = DailyStatsDTO.builder()
                        .date(date.toString())
                        .views(dayStats.get("views"))
                        .twice(dayStats.get("twice"))
                        .likes(dayStats.get("like"))
                        .comments(dayStats.get("comments"))
                        .build();
                }
                
                dailyStats.add(dailyStat);

                // 累计总数
                totalViews += dailyStat.getViews();
                totalTwice += dailyStat.getTwice();
                totalLikes += dailyStat.getLikes();
                totalComments += dailyStat.getComments();
            }

            return UserStatsDTO.builder()
                .userId(userId)
                .views(totalViews)
                .twices(totalTwice)
                .likes(totalLikes)
                .comments(totalComments)
                .build();
                
        } catch (Exception e) {
            log.error("获取用户{}时间段统计失败: days={}", userId, days, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 从Redis数据解析用户统计
     * 
     * 解析Redis中的原始统计数据:
     * 1. 遍历Redis Hash中的所有字段
     * 2. 过滤出属于指定用户的数据（通过userId前缀）
     * 3. 按统计类型分类累加
     * 4. 构造UserStatsDTO对象
     * 
     * @param userId 用户ID
     * @param stats Redis中的原始统计数据
     * @param date 日期字符串
     * @return 解析后的用户统计对象
     */
    private UserStatsDTO parseUserStatsFromRedis(long userId, Map<Object, Object> stats, String date) {
        int totalViews = 0;
        int totalTwice = 0;
        int totalLikes = 0;
        int totalComments = 0;

        String userPrefix = userId + ":";  // Redis field格式: "userId:statType"

        for (Map.Entry<Object, Object> entry : stats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());

            if (!field.startsWith(userPrefix)) continue;

            String statType = field.substring(userPrefix.length());
            switch (statType) {
                case STAT_TYPE_VIEW:
                    totalViews += count;
                    break;
                case STAT_TYPE_TWICE:
                    totalTwice += count;
                    break;
                case STAT_TYPE_LIKE:
                    totalLikes += count;
                    break;
                case STAT_TYPE_COMMENT:
                    totalComments += count;
                    break;
            }
        }

        return UserStatsDTO.builder()
            .userId(userId)
            .views(totalViews)
            .twices(totalTwice)
            .likes(totalLikes)
            .comments(totalComments)
            .build();
    }

    /**
     * 获取用户指定日期的统计数据
     * 
     * 从数据库JSON字段中提取特定日期的统计数据
     * 
     * @param userId 用户ID
     * @param date 查询日期
     * @return 当日各项统计数据的映射，key为统计类型，value为数值
     */
    public Map<String, Integer> getUserDayStats(long userId, LocalDate date) {
        validateUserId(userId);
        validateDate(date);
        try {
            int year = date.getYear();
            String dayKey = generateDayKey(date);

            // 使用MySQL JSON_EXTRACT函数直接查询特定日期的数据
            String dayStatsJson = userStatsYearlyMapper.getDayStats(userId, year, dayKey);

            if (dayStatsJson != null) {
                // 解析JSON数组为Map对象
                // JSON_ARRAY格式：[views, twice, like, comments]
                try {
                    List<Integer> statsArray = objectMapper.readValue(dayStatsJson, new TypeReference<List<Integer>>() {});
                    if (statsArray != null && statsArray.size() >= 4) {
                        Map<String, Integer> statsMap = new HashMap<>();
                        statsMap.put("views", statsArray.get(0));
                        statsMap.put("twice", statsArray.get(1));
                        statsMap.put("like", statsArray.get(2));
                        statsMap.put("comments", statsArray.get(3));
                        return statsMap;
                    }
                } catch (Exception jsonEx) {
                    log.error("JSON数组解析失败: userId={}, date={}, json={}", userId, date, dayStatsJson, jsonEx);
                    throw StatusCode.JSON_PROCESSING_ERROR.exception(jsonEx);
                }
            }

            // 返回空的统计数据，各项数值为0
            return createEmptyUserStatsMap();
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e; // 重新抛出业务异常（如JSON_PROCESSING_ERROR）
            }
            log.error("获取用户日统计失败: userId={}, date={}", userId, date, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 获取用户指定年份的完整统计数据
     * 
     * 返回用户整年的详细统计数据，包含每一天的记录
     * 
     * @param userId 用户ID
     * @param year 查询年份
     * @return 年度统计数据，外层key为dayKey（如"1-15"），内层key为统计类型
     */
    public Map<String, Map<String, Integer>> getUserYearStats(long userId, int year) {
        try {
            UserStatsYearlyDO yearStats = userStatsYearlyMapper.getByUserIdAndYear(userId, year);
            if (yearStats == null || yearStats.getStats() == null) {
                return new HashMap<>();
            }

            // 解析完整的年度JSON数据
            // 用户统计使用JSON_ARRAY格式：{"1-15": [views, twice, like, comments], "1-16": [...]}
            Map<String, List<Integer>> yearStatsArray = objectMapper.readValue(yearStats.getStats(),
                new TypeReference<Map<String, List<Integer>>>() {});

            // 转换为Map<String, Map<String, Integer>>格式以保持接口一致
            Map<String, Map<String, Integer>> result = new HashMap<>();
            for (Map.Entry<String, List<Integer>> entry : yearStatsArray.entrySet()) {
                String dayKey = entry.getKey();
                List<Integer> statsArray = entry.getValue();
                if (statsArray != null && statsArray.size() >= 4) {
                    Map<String, Integer> dayStats = new HashMap<>();
                    dayStats.put("views", statsArray.get(0));
                    dayStats.put("twice", statsArray.get(1));
                    dayStats.put("like", statsArray.get(2));
                    dayStats.put("comments", statsArray.get(3));
                    result.put(dayKey, dayStats);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("获取用户年度统计失败: userId={}, year={}", userId, year, e);
            return new HashMap<>();
        }
    }

    /**
     * 获取用户指定月份的统计汇总
     * 
     * 将指定月份的每日数据进行汇总，返回月度总计
     * 
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 月度汇总统计数据
     */
    public Map<String, Integer> getUserMonthStats(Long userId, int year, int month) {
        try {
            Map<String, Map<String, Integer>> yearStats = getUserYearStats(userId, year);
            Map<String, Integer> monthTotal = createEmptyUserStatsMap();

            // 遍历年度数据，筛选出指定月份的数据进行汇总
            for (Map.Entry<String, Map<String, Integer>> entry : yearStats.entrySet()) {
                String dayKey = entry.getKey(); // 格式："1-15"
                String[] parts = dayKey.split("-");
                if (parts.length == 2) {
                    int dayMonth = Integer.parseInt(parts[0]);
                    if (dayMonth == month) {
                        Map<String, Integer> dayStats = entry.getValue();
                        // 累加每日数据到月度总计
                        for (String statType : monthTotal.keySet()) {
                            monthTotal.put(statType, 
                                monthTotal.get(statType) + dayStats.getOrDefault(statType, 0));
                        }
                    }
                }
            }

            return monthTotal;
        } catch (Exception e) {
            log.error("获取用户月度统计失败: userId={}, year={}, month={}", userId, year, month, e);
            return createEmptyUserStatsMap();
        }
    }

    /**
     * 获取用户全部时间统计（历史数据 + 今日实时数据）
     * 
     * 设计思路：
     * 1. 昨天以前的所有历史数据 - 从缓存获取，一天最多计算一次
     * 2. 今天的实时数据 - 从Redis获取最新数据
     * 3. 两部分数据合并返回
     * 
     * 缓存策略：历史汇总数据按用户ID+截止日期缓存，避免重复计算
     * 
     * @param userId 用户ID
     * @return 全部时间的统计数据
     */
    @Cacheable(value = "allTimeStats", 
               key = "'user:' + #userId + ':' + T(java.time.LocalDate).now().minusDays(1).toString()", 
               unless = "#result == null")
    public UserStatsDTO getUserAllTimeStats(long userId) {
        try {
            // 1. 获取昨天以前的所有历史数据（带缓存）
            UserStatsDTO historicalStats = getUserHistoricalStats(userId);

            // 2. 获取今天的实时数据
            UserStatsDTO todayStats = getUserTodayStats(userId);

            // 3. 合并数据
            int totalViews = (historicalStats.getViews() != null ? historicalStats.getViews() : 0) +
                            (todayStats.getViews() != null ? todayStats.getViews() : 0);
            int totalTwices = (historicalStats.getTwices() != null ? historicalStats.getTwices() : 0) +
                             (todayStats.getTwices() != null ? todayStats.getTwices() : 0);
            int totalLikes = (historicalStats.getLikes() != null ? historicalStats.getLikes() : 0) +
                            (todayStats.getLikes() != null ? todayStats.getLikes() : 0);
            int totalComments = (historicalStats.getComments() != null ? historicalStats.getComments() : 0) +
                               (todayStats.getComments() != null ? todayStats.getComments() : 0);

            return UserStatsDTO.builder()
                .userId(userId)
                .views(totalViews)
                .twices(totalTwices)
                .likes(totalLikes)
                .comments(totalComments)
                .build();

        } catch (Exception e) {
            log.error("获取用户{}全部时间统计失败", userId, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户昨天以前的所有历史数据汇总
     * 
     * 该方法会被缓存，每天最多计算一次，避免重复的数据库查询和计算
     * 缓存key包含截止日期，确保每天的历史汇总数据独立
     * 
     * @param userId 用户ID
     * @return 历史数据汇总
     */
    @Cacheable(value = "historicalStats", 
               key = "'user:' + #userId + ':until:' + T(java.time.LocalDate).now().minusDays(1).toString()", 
               unless = "#result == null")
    public UserStatsDTO getUserHistoricalStats(long userId) {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate startDate = LocalDate.of(2020, 1, 1); // 系统开始日期

            // 使用现有的日期范围统计方法
            Map<String, Integer> totalStats = getUserDateRangeStats(userId, startDate, yesterday);

            return UserStatsDTO.builder()
                .userId(userId)
                .views(totalStats.get("views"))
                .twices(totalStats.get("twice"))
                .likes(totalStats.get("like"))
                .comments(totalStats.get("comments"))
                .build();

        } catch (Exception e) {
            log.error("获取用户{}历史统计数据失败", userId, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户指定日期范围的统计汇总
     * 
     * 支持跨年查询，将指定日期范围内的所有数据进行汇总
     * 
     * @param userId 用户ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 日期范围内的汇总统计数据
     */
    public Map<String, Integer> getUserDateRangeStats(long userId, LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Integer> total = createEmptyUserStatsMap();

            // 按年份分组查询，支持跨年统计
            int startYear = startDate.getYear();
            int endYear = endDate.getYear();

            for (int year = startYear; year <= endYear; year++) {
                Map<String, Map<String, Integer>> yearStats = getUserYearStats(userId, year);
                
                // 遍历年度数据，筛选出日期范围内的数据
                for (Map.Entry<String, Map<String, Integer>> entry : yearStats.entrySet()) {
                    String dayKey = entry.getKey();
                    LocalDate dayDate = parseDayKey(year, dayKey);
                    
                    // 检查日期是否在指定范围内
                    if (dayDate != null && 
                        (dayDate.isEqual(startDate) || dayDate.isAfter(startDate)) &&
                        (dayDate.isEqual(endDate) || dayDate.isBefore(endDate))) {
                        
                        Map<String, Integer> dayStats = entry.getValue();
                        // 累加符合条件的每日数据
                        for (String statType : total.keySet()) {
                            total.put(statType, 
                                total.get(statType) + dayStats.getOrDefault(statType, 0));
                        }
                    }
                }
            }

            return total;
        } catch (Exception e) {
            log.error("获取用户日期范围统计失败: userId={}, startDate={}, endDate={}", 
                userId, startDate, endDate, e);
            return createEmptyUserStatsMap();
        }
    }


    // ====== 私有辅助方法 ======

    /**
     * 创建空的用户统计数据映射
     * 
     * 初始化所有统计类型的数值为0，确保返回数据的一致性
     * 
     * @return 包含所有统计类型的空映射
     */
    private Map<String, Integer> createEmptyUserStatsMap() {
        Map<String, Integer> empty = new HashMap<>();
        empty.put("views", 0);
        empty.put("twice", 0);
        empty.put("like", 0);
        empty.put("comments", 0);
        return empty;
    }

    /**
     * 解析dayKey字符串为LocalDate对象
     * 
     * 将数据库中存储的dayKey（如"1-15"）转换为具体的日期对象
     * 
     * @param year 年份
     * @param dayKey 日期键，格式为"月-日"
     * @return 解析后的LocalDate对象，解析失败返回null
     */
    private LocalDate parseDayKey(int year, String dayKey) {
        try {
            String[] parts = dayKey.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            log.warn("解析dayKey失败: year={}, dayKey={}", year, dayKey, e);
        }
        return null;
    }



    // ========== 私有辅助方法 ==========

    /**
     * 从 Redis Hash 中获取整数字段值
     */
    private int getRedisHashFieldAsInt(String key, String field) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.debug("获取 Redis 字段失败: key={}, field={}, error={}", key, field, e.getMessage());
        }
        return 0;
    }

    /**
     * 生成用户统计Redis键名
     */
    private String generateUserStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + USER_STATS_SUFFIX;
    }

    /**
     * 生成文章统计Redis键名
     */
    private String generatePostStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + CONTENT_STATS_SUFFIX;
    }

    /**
     * 生成日期键（月-日格式）
     */
    private String generateDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }

    /**
     * 验证用户ID有效性
     */
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw StatusCode.USER_NOT_FOUND.exception();
        }
    }

// --注释掉检查 START (2025/12/10 11:34):
//    /**
//     * 验证文章ID有效性
//     */
//    private void validatePostId(long postId) {
//        if (postId <= 0) {
//            throw ErrorCode.CONTENTS_POST_NOT_FOUND.exception();
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:34)

    /**
     * 验证日期有效性
     */
    private void validateDate(LocalDate date) {
        if (date == null) {
            throw StatusCode.INVALID_DATE.exception();
        }
        LocalDate systemStart = LocalDate.parse(systemProperties.getStats().getSystemStartDate());
        if (date.isBefore(systemStart) || date.isAfter(LocalDate.now())) {
            throw StatusCode.INVALID_DATE.exception();
        }
    }

    /**
     * 验证天数范围有效性
     */
    private void validateDaysRange(int days) {
        if (days <= 0 || days > systemProperties.getStats().getMaxQueryDaysRange()) {
            throw StatusCode.INVALID_DAYS_RANGE.exception();
        }
    }

    // ========== 新方法：返回统计数据而不是修改 DTO ==========

    /**
     * 批量获取内容统计数据
     *
     * 逻辑：
     * - 按日统计字段（views, twice, likes, comments）: content_stats 累计 + 今日 Redis 增量
     * - 非按日统计字段（shares, bookmarks, completedUsers, inProgressUsers）: 直接从 content_stats 获取
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return Map<ContentId, ContentStatsDTO>
     */
    public Map<Long, ContentStatsDTO> batchGetContentStats(ContentType contentType, List<Long> contentIds) {
        Map<Long, ContentStatsDTO> result = new HashMap<>();

        if (contentIds == null || contentIds.isEmpty()) {
            return result;
        }

        log.debug("批量获取 {} 个{}的统计数据", contentIds.size(), contentType);

        try {
            // 1. 批量查询 content_stats 表的累计数据
            List<ContentStatsDO> contentStatsList =
                contentStatsDataService.batchGetByContentIds(contentType, contentIds);

            Map<Long, ContentStatsDO> statsMap = contentStatsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats));

            // 2. 批量获取今日 Redis 实时增量（只有 views, twice, likes, comments 四个字段）
            Map<Long, DailyStatsDTO> todayStatsMap = batchGetTodayStatsForContent(contentType, contentIds);

            // 3. 组装返回
            for (Long contentId : contentIds) {
                ContentStatsDO baseStats = statsMap.get(contentId);
                DailyStatsDTO todayStats = todayStatsMap.get(contentId);

                // 按日统计字段：累计 + 今日增量
                int baseViews = baseStats != null && baseStats.getViews() != null ? baseStats.getViews() : 0;
                int baseTwice = baseStats != null && baseStats.getTwices() != null ? baseStats.getTwices() : 0;
                int baseLikes = baseStats != null && baseStats.getLikes() != null ? baseStats.getLikes() : 0;
                int baseComments = baseStats != null && baseStats.getComments() != null ? baseStats.getComments() : 0;

                int todayViews = todayStats != null && todayStats.getViews() != null ? todayStats.getViews() : 0;
                int todayTwice = todayStats != null && todayStats.getTwice() != null ? todayStats.getTwice() : 0;
                int todayLikes = todayStats != null && todayStats.getLikes() != null ? todayStats.getLikes() : 0;
                int todayComments = todayStats != null && todayStats.getComments() != null ? todayStats.getComments() : 0;

                // 非按日统计字段：直接从 content_stats 获取（不加今日增量）
                int baseShares = baseStats != null && baseStats.getShares() != null ? baseStats.getShares() : 0;
                int baseBookmarks = baseStats != null && baseStats.getBookmarks() != null ? baseStats.getBookmarks() : 0;
                int baseCompleted = baseStats != null && baseStats.getCompletedUsers() != null ? baseStats.getCompletedUsers() : 0;
                int baseInProgress = baseStats != null && baseStats.getInProgressUsers() != null ? baseStats.getInProgressUsers() : 0;

                // 构建 DTO
                ContentStatsDTO dto = ContentStatsDTO.builder()
                    .contentId(contentId)
                    .views(baseViews + todayViews)              // 累计 + 今日增量
                    .twiceUpvotes(baseTwice + todayTwice)       // 累计 + 今日增量
                    .likeUpvotes(baseLikes + todayLikes)        // 累计 + 今日增量
                    .comments(baseComments + todayComments)     // 累计 + 今日增量
                    .shares(baseShares)                         // 只用累计值
                    .bookmarks(baseBookmarks)                   // 只用累计值
                    .completedUsers(baseCompleted)              // 只用累计值
                    .inProgressUsers(baseInProgress)            // 只用累计值
                    .build();

                result.put(contentId, dto);
            }

            log.debug("成功获取 {} 个{}的统计数据", contentIds.size(), contentType);

        } catch (Exception e) {
            log.error("批量获取统计数据失败: contentType={}", contentType, e);
        }

        return result;
    }

    /**
     * 批量获取今日 Redis 实时统计增量
     *
     * 注意：只包含按日统计的字段（views, twice, likes, comments）
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return Map<ContentId, DailyStatsDTO>
     */
    private Map<Long, DailyStatsDTO> batchGetTodayStatsForContent(ContentType contentType, List<Long> contentIds) {
        Map<Long, DailyStatsDTO> result = new HashMap<>();

        String today = LocalDate.now().toString();
        String redisKey = STATS_KEY_PREFIX + today + CONTENT_STATS_SUFFIX;

        // 检查 Redis key 是否存在
        Boolean keyExists = redisTemplate.hasKey(redisKey);
        if (keyExists == null || !keyExists) {
            log.debug("今日 Redis 统计数据不存在: {}", redisKey);
            return result;
        }

        int contentTypeValue = contentType.value();

        // 批量获取今日各项统计（只有4个按日统计的字段）
        for (Long contentId : contentIds) {
            // Redis 字段格式：contentType:contentId:statType
            // 例如：1:123:view (ContentType.post=1, contentId=123, statType=view)
            int views = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_VIEW);
            int twice = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_TWICE);
            int likes = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_LIKE);
            int comments = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_COMMENT);

            DailyStatsDTO stats = DailyStatsDTO.builder()
                .views(views)
                .twice(twice)
                .likes(likes)
                .comments(comments)
                .build();

            result.put(contentId, stats);
        }

        return result;
    }
}
