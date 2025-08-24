package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.dto.DailyStatsDTO;
import com.prosper.learn.dto.UserStatsDTO;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
import com.prosper.learn.persistence.dataobject.PostStatsDO;
import com.prosper.learn.persistence.mapper.PostStatsMapper;
import com.prosper.learn.persistence.mapper.UserStatsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日常统计数据同步服务
 * 
 * 该服务负责将Redis中的实时统计数据同步到数据库进行持久化存储，同时提供统计数据的查询功能。
 * 
 * 设计思路:
 * 1. 实时统计数据存储在Redis中，性能高但非持久化
 * 2. 定期将Redis数据同步到数据库，确保数据不丢失
 * 3. 支持补偿机制，处理同步失败的情况
 * 4. 提供统一的用户和文章统计数据查询接口
 * 
 * Redis数据结构:
 * - 用户统计: stats:YYYY-MM-DD:user -> {userId:statType: count}
 * - 文章统计: stats:YYYY-MM-DD:post -> {postId:statType: count}
 * 
 * 数据库存储结构（JSON格式）:
 * - 用户统计: user_stats.stats -> {"1-1":{"views": 10, "twice": 3, "helpful": 2, "comments": 5}, ...}
 * - 文章统计: post_stats.stats -> {"1-1":{"views": 100, "twice": 3, "helpful": 2, "comments": 8}, ...}
 * 
 * 统计类型包括:
 * - view/views: 浏览量
 * - twice: 两次能懂
 * - helpful: 有用点赞
 * - comment/comments: 评论数
 */
@Slf4j
@Service
public class DailyStatsService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private PostStatsMapper postStatsMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 补偿同步任务 - 检查最近7天是否有同步失败的
     * 
     * 容错机制，处理因为系统故障等原因导致的同步失败:
     * 1. 检查最近7天的Redis数据，正常情况下应该已被同步并删除
     * 2. 如果发现遗留数据，说明之前的同步失败了
     * 3. 执行补偿同步，确保数据不丢失
     */
    public void compensationSync() {
        log.info("开始补偿同步任务");
        
        for (int i = 1; i <= 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.toString();
            
            // 检查Redis中是否还有这个日期的数据
            String userKey = "stats:" + dateStr + ":user";
            String postKey = "stats:" + dateStr + ":post";
            
            boolean hasUserData = redisTemplate.hasKey(userKey);
            boolean hasPostData = redisTemplate.hasKey(postKey);
            
            if (hasUserData || hasPostData) {
                log.warn("发现{}的数据未同步，开始补偿同步", dateStr);
                try {
                    if (hasUserData) {
                        int userCount = syncUserStats(dateStr);
                        log.info("补偿同步{}的用户数据: {}条", dateStr, userCount);
                    }
                    
                    if (hasPostData) {
                        int postCount = syncPostStats(dateStr);
                        log.info("补偿同步{}的文章数据: {}条", dateStr, postCount);
                    }
                } catch (Exception e) {
                    log.error("补偿同步{}失败", dateStr, e);
                }
            }
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
        String userKey = "stats:" + dateStr + ":user";
        Map<Object, Object> userStats = redisTemplate.opsForHash().entries(userKey);
        
        if (userStats.isEmpty()) {
            log.info("{}没有用户统计数据", dateStr);
            return 0;
        }
        
        // 解析日期信息
        LocalDate date = LocalDate.parse(dateStr);
        int year = date.getYear();
        String dayKey = date.getMonthValue() + "-" + date.getDayOfMonth();
        
        // 按用户ID分组并聚合当天的完整统计数据
        Map<Integer, UserDayStats> userDayStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : userStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (count <= 0) continue; // 跳过无效数据
            
            String[] parts = field.split(":");
            if (parts.length != 2) continue;
            
            Integer userId = Integer.parseInt(parts[0]);
            String statType = parts[1];
            
            UserDayStats dayStats = userDayStatsMap.computeIfAbsent(userId, k -> new UserDayStats());
            
            // 聚合当天的完整数据
            switch (statType) {
                case "view":
                    dayStats.views += count;
                    break;
                case "twice":
                    dayStats.twice += count;
                    break;
                case "helpful":
                    dayStats.helpful += count;
                    break;
                case "comment":
                    dayStats.comments += count;
                    break;
                default:
                    log.debug("忽略未知的统计类型: {}", statType);
            }
        }
        
        // 直接覆盖数据库中当天的数据
        int updateCount = 0;
        for (Map.Entry<Integer, UserDayStats> entry : userDayStatsMap.entrySet()) {
            Integer userId = entry.getKey();
            UserDayStats dayStats = entry.getValue();
            
            try {
                // 确保用户的年度记录存在
                ensureUserYearRecord(userId, year);
                
                // 直接设置当天的完整数据（覆盖而非增量）
                int updated = userStatsMapper.setUserDayStats(
                        userId, year, dayKey,
                        dayStats.views, dayStats.twice, dayStats.helpful,
                        dayStats.comments);
                
                if (updated > 0) {
                    log.debug("覆盖用户{}在{}的统计数据: views={}, twice={}, helpful={}, comments={}", 
                        userId, dateStr, dayStats.views, dayStats.twice, dayStats.helpful, dayStats.comments);
                    updateCount++;
                } else {
                    log.warn("用户{}的{}年度记录不存在，无法更新统计", userId, year);
                }
                
            } catch (Exception e) {
                log.error("同步用户{}在{}的统计数据失败", userId, dateStr, e);
            }
        }
        
        // 删除Redis数据，释放内存空间
        redisTemplate.delete(userKey);
        log.info("完成{}的用户统计数据同步，共更新{}个用户", dateStr, updateCount);
        
        return updateCount;
    }
    
    /**
     * 用户单日统计数据结构
     */
    private static class UserDayStats {
        int views = 0;
        int twice = 0;
        int helpful = 0;
        int comments = 0;
    }
    
    /**
     * 确保用户的年度统计记录存在
     */
    private void ensureUserYearRecord(Integer userId, int year) {
        UserStatsDO existing = userStatsMapper.getByUserIdAndYear(userId, year);
        if (existing == null) {
            UserStatsDO yearRecord = new UserStatsDO();
            yearRecord.setUserId(userId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}"); // 初始化为空JSON对象
            userStatsMapper.insert(yearRecord);
            log.debug("创建用户{}的{}年度统计记录", userId, year);
        }
    }

    /**
     * 同步文章统计数据
     * 
     * 处理逻辑:
     * 1. 从Redis获取指定日期的文章统计数据
     * 2. 解析field格式: "postId:statType" -> count
     * 3. 按文章ID分组聚合各种统计类型的数据
     * 4. 更新post_stats表中的JSON字段
     * 5. 删除Redis中的原始数据
     * 
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    private int syncPostStats(String dateStr) {
        String postKey = "stats:" + dateStr + ":post";
        Map<Object, Object> postStats = redisTemplate.opsForHash().entries(postKey);
        
        if (postStats.isEmpty()) {
            log.info("{}没有文章统计数据", dateStr);
            return 0;
        }
        
        // 按文章ID分组
        Map<Long, Map<String, Integer>> postStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : postStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (count <= 0) continue;
            
            String[] parts = field.split(":");
            if (parts.length != 2) continue;
            
            Long postId = Long.parseLong(parts[0]);
            String statType = parts[1];
            
            postStatsMap.computeIfAbsent(postId, k -> new HashMap<>())
                .put(statType, count);
        }
        
        // 更新post_stats表
        int updateCount = 0;
        int year = LocalDate.parse(dateStr).getYear();
        String dayKey = dateStr.substring(5).replace("-", "-"); // "08-22"
        
        for (Map.Entry<Long, Map<String, Integer>> entry : postStatsMap.entrySet()) {
            Long postId = entry.getKey();
            Map<String, Integer> stats = entry.getValue();
            
            try {
                for (Map.Entry<String, Integer> statEntry : stats.entrySet()) {
                    String statType = statEntry.getKey();
                    Integer count = statEntry.getValue();
                    
                    // 使用PostStatsMapper更新数据库
                    updatePostStatsField("POST", postId, year, dayKey, statType, count);
                }
                updateCount++;
                log.debug("更新文章{}在{}的统计数据", postId, dateStr);
            } catch (Exception e) {
                log.error("同步文章{}在{}的统计数据失败", postId, dateStr, e);
            }
        }
        
        // 同步成功后删除Redis数据
        redisTemplate.delete(postKey);
        log.info("删除Redis中{}的文章统计数据", dateStr);
        
        return updateCount;
    }

    /**
     * 更新post_stats表中的字段
     * 
     * 该方法负责将Redis中的统计数据写入数据库的post_stats表:
     * 1. 确保年度记录存在
     * 2. 使用PostStatsMapper进行原子性的数值设置
     * 3. 支持直接覆盖，确保数据一致性
     * 
     * @param type 对象类型，如"POST", "NODE", "ROADMAP"
     * @param objectId 对象ID
     * @param year 统计年份
     * @param dayKey 日期键，格式如"8-22"
     * @param field 统计字段，如"view", "twice", "helpful", "comment"
     * @param count 统计数值
     */
    private void updatePostStatsField(String type, Long objectId, int year, String dayKey, String field, int count) {
        try {
            // 确保post_stats年度记录存在
            ensurePostYearRecord(type, objectId, year);
            
            // 使用PostStatsMapper直接设置当天的统计值
            Map<String, Integer> dayStats = getCurrentPostDayStats(type, objectId, year, dayKey);
            dayStats.put(field, count);
            
            int updated = postStatsMapper.setDayStats(type, objectId, year, dayKey,
                    dayStats.getOrDefault("views", 0),
                    dayStats.getOrDefault("twice", 0),
                    dayStats.getOrDefault("helpful", 0),
                    dayStats.getOrDefault("comments", 0));
            
            if (updated > 0) {
                log.debug("设置post_stats: type={}, objectId={}, dayKey={}, field={}, count={}", 
                    type, objectId, dayKey, field, count);
            } else {
                log.warn("post_stats记录不存在: type={}, objectId={}, year={}", type, objectId, year);
            }
                
        } catch (Exception e) {
            log.error("更新post_stats失败: type={}, objectId={}, field={}, count={}", 
                type, objectId, field, count, e);
        }
    }
    
    /**
     * 确保post_stats的年度记录存在
     */
    private void ensurePostYearRecord(String type, Long objectId, int year) {
        PostStatsDO existing = postStatsMapper.getByTypeAndObjectIdAndYear(type, objectId, year);
        if (existing == null) {
            PostStatsDO yearRecord = new PostStatsDO();
            yearRecord.setType(type);
            yearRecord.setObjectId(objectId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}");
            postStatsMapper.insert(yearRecord);
            log.debug("创建{}对象{}的{}年度统计记录", type, objectId, year);
        }
    }
    
    /**
     * 获取当前post指定日期的统计数据
     */
    private Map<String, Integer> getCurrentPostDayStats(String type, Long objectId, int year, String dayKey) {
        try {
            String dayStatsJson = postStatsMapper.getDayStats(type, objectId, year, dayKey);
            if (dayStatsJson != null) {
                return objectMapper.readValue(dayStatsJson, new TypeReference<Map<String, Integer>>() {});
            }
        } catch (Exception e) {
            log.debug("获取post当日统计失败，使用默认值: type={}, objectId={}, dayKey={}", type, objectId, dayKey);
        }
        
        Map<String, Integer> defaultStats = new HashMap<>();
        defaultStats.put("views", 0);
        defaultStats.put("twice", 0);
        defaultStats.put("helpful", 0);
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
     * 使用缓存注解提高查询性能
     */
    @Cacheable(value = "todayStats", key = "'user:' + #userId", unless = "#result == null")
    public UserStatsDTO getUserTodayStats(Integer userId) {
        String today = LocalDate.now().toString();
        String userKey = "stats:" + today + ":user";
        
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
     */
    @Cacheable(value = "yesterdayStats", key = "'user:' + #userId", unless = "#result == null")
    public UserStatsDTO getUserYesterdayStats(Integer userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        try {
            Map<String, Integer> stats = getUserDayStats(userId, yesterday);
            
            UserStatsDTO dto = UserStatsDTO.builder()
                .userId(userId)
                .period("yesterday")
                .startDate(yesterday)
                .endDate(yesterday)
                .totalViews(stats.get("views").longValue())
                .totalTwice(stats.get("twice").longValue())
                .totalHelpful(stats.get("helpful").longValue())
                .totalComments(stats.get("comments").longValue())
                .build();
                
            return dto;
        } catch (Exception e) {
            log.error("获取用户{}昨日统计失败", userId, e);
            return UserStatsDTO.empty();
        }
    }

    /**
     * 获取用户历史统计（从数据库）
     */
    @Cacheable(value = "historyStats", key = "'user:' + #userId + ':' + #days", unless = "#result == null")
    public UserStatsDTO getUserHistoryStats(Integer userId, int days) {
        LocalDate endDate = LocalDate.now().minusDays(1); // 不包括今天
        LocalDate startDate = endDate.minusDays(days - 1);
        
        try {
            Map<String, Integer> totalStats = getUserDateRangeStats(userId, startDate, endDate);
            
            UserStatsDTO dto = UserStatsDTO.builder()
                .userId(userId)
                .period(days + "days")
                .startDate(startDate)
                .endDate(endDate)
                .totalViews(totalStats.get("views").longValue())
                .totalTwice(totalStats.get("twice").longValue())
                .totalHelpful(totalStats.get("helpful").longValue())
                .totalComments(totalStats.get("comments").longValue())
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
    public UserStatsDTO getUserPeriodStatsWithDaily(Integer userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        try {
            List<DailyStatsDTO> dailyStats = new ArrayList<>();
            
            // 总计数据
            long totalViews = 0;
            long totalTwice = 0;
            long totalHelpful = 0;
            long totalComments = 0;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                DailyStatsDTO dailyStat;
                
                if (date.equals(LocalDate.now())) {
                    // 今日数据从Redis获取
                    UserStatsDTO todayStats = getUserTodayStats(userId);
                    dailyStat = DailyStatsDTO.builder()
                        .date(date)
                        .views(todayStats.getTotalViews())
                        .twice(todayStats.getTotalTwice())
                        .helpful(todayStats.getTotalHelpful())
                        .comments(todayStats.getTotalComments())
                        .build();
                } else {
                    // 历史数据从数据库获取
                    Map<String, Integer> dayStats = getUserDayStats(userId, date);
                        
                    dailyStat = DailyStatsDTO.builder()
                        .date(date)
                        .views(dayStats.get("views").longValue())
                        .twice(dayStats.get("twice").longValue())
                        .helpful(dayStats.get("helpful").longValue())
                        .comments(dayStats.get("comments").longValue())
                        .build();
                }
                
                dailyStats.add(dailyStat);
                
                // 累计总数
                totalViews += dailyStat.getViews();
                totalTwice += dailyStat.getTwice();
                totalHelpful += dailyStat.getHelpful();
                totalComments += dailyStat.getComments();
            }
            
            return UserStatsDTO.builder()
                .userId(userId)
                .period(days + "days")
                .startDate(startDate)
                .endDate(endDate)
                .totalViews(totalViews)
                .totalTwice(totalTwice)
                .totalHelpful(totalHelpful)
                .totalComments(totalComments)
                .dailyStats(dailyStats)
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
    private UserStatsDTO parseUserStatsFromRedis(Integer userId, Map<Object, Object> stats, String date) {
        long totalViews = 0;
        long totalTwice = 0;
        long totalHelpful = 0;
        long totalComments = 0;
        
        String userPrefix = userId + ":";  // Redis field格式: "userId:statType"
        
        for (Map.Entry<Object, Object> entry : stats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (!field.startsWith(userPrefix)) continue;
            
            String statType = field.substring(userPrefix.length());
            switch (statType) {
                case "view":
                    totalViews += count;
                    break;
                case "twice":
                    totalTwice += count;
                    break;
                case "helpful":
                    totalHelpful += count;
                    break;
                case "comment":
                    totalComments += count;
                    break;
            }
        }
        
        return UserStatsDTO.builder()
            .userId(userId)
            .period("today")
            .startDate(LocalDate.parse(date))
            .endDate(LocalDate.parse(date))
            .totalViews(totalViews)
            .totalTwice(totalTwice)
            .totalHelpful(totalHelpful)
            .totalComments(totalComments)
            .build();
    }

    // ====== 用户统计服务方法 ======
    // 以下方法提供用户统计数据的管理功能，支持实时统计和查询

    /**
     * 记录用户统计事件（实时增量更新）
     * 
     * 用于实时记录用户行为统计，直接更新数据库中的JSON数据
     * 适用场景：用户实时操作，如点赞、评论等需要立即反映的统计
     * 
     * 工作原理：
     * 1. 确保用户的年度统计记录存在
     * 2. 使用MySQL JSON函数进行原子性的增量更新
     * 3. 直接修改数据库，不经过Redis
     * 
     * @param userId 用户ID
     * @param statType 统计类型 (views, twice, helpful, comments)
     * @param count 增加的数量，通常为1
     */
    public void recordUserStats(Integer userId, String statType, int count) {
        try {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            String dayKey = today.getMonthValue() + "-" + today.getDayOfMonth();

            // 确保用户年度记录存在
            ensureUserYearRecord(userId, currentYear);

            // 使用MySQL JSON操作直接更新，支持并发安全
            int updated = userStatsMapper.incrementUserStatsCount(userId, currentYear, dayKey, statType, count);

            if (updated > 0) {
                log.debug("实时更新用户{}的{}统计: +{}", userId, statType, count);
            } else {
                log.warn("更新用户{}统计失败，年度记录可能不存在: year={}", userId, currentYear);
            }

        } catch (Exception e) {
            log.error("记录用户实时统计失败: userId={}, statType={}, count={}", userId, statType, count, e);
        }
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
    public Map<String, Integer> getUserDayStats(Integer userId, LocalDate date) {
        try {
            int year = date.getYear();
            String dayKey = date.getMonthValue() + "-" + date.getDayOfMonth();

            // 使用MySQL JSON_EXTRACT函数直接查询特定日期的数据
            String dayStatsJson = userStatsMapper.getDayStats(userId, year, dayKey);

            if (dayStatsJson != null) {
                // 解析JSON字符串为Map对象
                return objectMapper.readValue(dayStatsJson, new TypeReference<Map<String, Integer>>() {});
            }

            // 返回空的统计数据，各项数值为0
            return createEmptyUserStatsMap();
        } catch (Exception e) {
            log.error("获取用户日统计失败: userId={}, date={}", userId, date, e);
            return createEmptyUserStatsMap();
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
    public Map<String, Map<String, Integer>> getUserYearStats(Integer userId, int year) {
        try {
            UserStatsDO yearStats = userStatsMapper.getByUserIdAndYear(userId, year);
            if (yearStats == null || yearStats.getStats() == null) {
                return new HashMap<>();
            }

            // 解析完整的年度JSON数据
            return objectMapper.readValue(yearStats.getStats(), 
                new TypeReference<Map<String, Map<String, Integer>>>() {});
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
    public Map<String, Integer> getUserMonthStats(Integer userId, int year, int month) {
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
     * 获取用户指定日期范围的统计汇总
     * 
     * 支持跨年查询，将指定日期范围内的所有数据进行汇总
     * 
     * @param userId 用户ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 日期范围内的汇总统计数据
     */
    public Map<String, Integer> getUserDateRangeStats(Integer userId, LocalDate startDate, LocalDate endDate) {
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

    /**
     * 删除用户指定年份之前的旧统计数据
     * 
     * 用于数据清理，删除过期的统计记录以节省存储空间
     * 
     * @param beforeYear 删除此年份之前的数据
     * @return 删除的记录数
     */
    public int deleteOldUserStats(int beforeYear) {
        try {
            int deleted = userStatsMapper.deleteOldStats(beforeYear);
            log.info("删除{}年之前的用户统计数据: {}条", beforeYear, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("删除旧用户统计数据失败: beforeYear={}", beforeYear, e);
            return 0;
        }
    }

    /**
     * 获取指定年份的所有用户ID列表
     * 
     * 用于数据分析和批量处理
     * 
     * @param year 查询年份
     * @return 该年份有统计数据的用户ID列表
     */
    public java.util.List<Integer> getUserIdsByYear(int year) {
        try {
            return userStatsMapper.getUserIdsByYear(year);
        } catch (Exception e) {
            log.error("获取年度用户ID列表失败: year={}", year, e);
            return java.util.List.of();
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
        empty.put("helpful", 0);
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
}