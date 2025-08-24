package com.prosper.learn.domain.service;

import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.DailyStatsDTO;
import com.prosper.learn.dto.UserStatsDTO;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
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
 * 该服务负责将Redis中的实时统计数据同步到数据库进行持久化存储。
 * 
 * 设计思路:
 * 1. 实时统计数据存储在Redis中，性能高但非持久化
 * 2. 定期将Redis数据同步到数据库，确保数据不丢失
 * 3. 支持补偿机制，处理同步失败的情况
 * 
 * Redis数据结构:
 * - 用户统计: stats:YYYY-MM-DD:user -> {userId:statType: count}
 * - 文章统计: stats:YYYY-MM-DD:article -> {articleId:statType: count}
 * 
 * 统计类型包括:
 * - view: 浏览量
 * - once/twice/helpful: 不同类型的点赞
 * - comment: 评论数
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
            
            // 同步文章统计到upvote_stats表
            int articleStatsCount = syncArticleStats(dateStr);
            
            log.info("同步{}的数据完成: 用户统计{}条, 文章统计{}条", dateStr, userStatsCount, articleStatsCount);
            
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
            String articleKey = "stats:" + dateStr + ":article";
            
            boolean hasUserData = redisTemplate.hasKey(userKey);
            boolean hasArticleData = redisTemplate.hasKey(articleKey);
            
            if (hasUserData || hasArticleData) {
                log.warn("发现{}的数据未同步，开始补偿同步", dateStr);
                try {
                    if (hasUserData) {
                        int userCount = syncUserStats(dateStr);
                        log.info("补偿同步{}的用户数据: {}条", dateStr, userCount);
                    }
                    
                    if (hasArticleData) {
                        int articleCount = syncArticleStats(dateStr);
                        log.info("补偿同步{}的文章数据: {}条", dateStr, articleCount);
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
     * 处理逻辑:
     * 1. 从Redis获取指定日期的用户统计数据
     * 2. 解析field格式: "userId:statType" -> count
     * 3. 按用户ID分组聚合各种统计类型的数据
     * 4. 写入user_stats表，支持增量更新
     * 5. 删除Redis中的原始数据
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
        
        // 按用户ID分组统计，将Redis中的扁平数据转换为结构化的用户统计对象
        Map<Integer, UserStatsDO> userStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : userStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (count <= 0) continue; // 跳过无效数据
            
            String[] parts = field.split(":");
            if (parts.length != 2) continue;
            
            Integer userId = Integer.parseInt(parts[0]);
            String statType = parts[1];
            
            UserStatsDO userStat = userStatsMap.computeIfAbsent(userId, k -> {
                UserStatsDO stats = new UserStatsDO();
                stats.setUserId(userId);
                stats.setStatDate(LocalDate.parse(dateStr));
                stats.setTotalViews(0L);
                stats.setTotalTwice(0L);
                stats.setTotalHelpful(0L);
                stats.setTotalComments(0L);
                return stats;
            });
            
            // 累加对应的统计数据
            switch (statType) {
                case "view":
                    userStat.setTotalViews(userStat.getTotalViews() + count.longValue());
                    break;
                case "twice":
                    userStat.setTotalTwice(userStat.getTotalTwice() + count.longValue());
                    break;
                case "helpful":
                    userStat.setTotalHelpful(userStat.getTotalHelpful() + count.longValue());
                    break;
                case "comment":
                    userStat.setTotalComments(userStat.getTotalComments() + count.longValue());
                    break;
                default:
                    log.debug("忽略未知的统计类型: {}", statType);
            }
        }
        
        // 批量写入数据库
        int insertCount = 0;
        for (UserStatsDO userStat : userStatsMap.values()) {
            try {
                // 先尝试查询是否已存在
                UserStatsDO existing = userStatsMapper.getByUserIdAndDate(
                    userStat.getUserId(), userStat.getStatDate());
                
                if (existing == null) {
                    userStatsMapper.insert(userStat);
                    insertCount++;
                    log.debug("插入用户{}在{}的统计数据", userStat.getUserId(), dateStr);
                } else {
                    // 累加到现有数据
                    existing.setTotalViews(existing.getTotalViews() + userStat.getTotalViews());
                    existing.setTotalTwice(existing.getTotalTwice() + userStat.getTotalTwice());
                    existing.setTotalHelpful(existing.getTotalHelpful() + userStat.getTotalHelpful());
                    existing.setTotalComments(existing.getTotalComments() + userStat.getTotalComments());
                    
                    userStatsMapper.update(existing);
                    insertCount++;
                    log.debug("更新用户{}在{}的统计数据", userStat.getUserId(), dateStr);
                }
            } catch (Exception e) {
                log.error("同步用户{}在{}的统计数据失败", userStat.getUserId(), dateStr, e);
            }
        }
        
        // 同步成功后删除Redis数据
        redisTemplate.delete(userKey);
        log.info("删除Redis中{}的用户统计数据", dateStr);
        
        return insertCount;
    }

    /**
     * 同步文章统计数据到post_stats表
     * 
     * 处理逻辑:
     * 1. 从Redis获取指定日期的文章统计数据
     * 2. 解析field格式: "articleId:statType" -> count
     * 3. 按文章ID分组聚合各种统计类型的数据
     * 4. 更新post_stats表中的JSON字段
     * 5. 删除Redis中的原始数据
     * 
     * 注意: 目前updateUpvoteStatsField方法只有日志记录，具体的数据库更新逻辑需要实现
     * 
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    private int syncArticleStats(String dateStr) {
        String articleKey = "stats:" + dateStr + ":article";
        Map<Object, Object> articleStats = redisTemplate.opsForHash().entries(articleKey);
        
        if (articleStats.isEmpty()) {
            log.info("{}没有文章统计数据", dateStr);
            return 0;
        }
        
        // 按文章ID分组
        Map<Long, Map<String, Integer>> articleStatsMap = new HashMap<>();
        
        for (Map.Entry<Object, Object> entry : articleStats.entrySet()) {
            String field = (String) entry.getKey();
            Integer count = Integer.parseInt((String) entry.getValue());
            
            if (count <= 0) continue;
            
            String[] parts = field.split(":");
            if (parts.length != 2) continue;
            
            Long articleId = Long.parseLong(parts[0]);
            String statType = parts[1];
            
            articleStatsMap.computeIfAbsent(articleId, k -> new HashMap<>())
                .put(statType, count);
        }
        
        // 更新upvote_stats表
        int updateCount = 0;
        int year = LocalDate.parse(dateStr).getYear();
        String dayKey = dateStr.substring(5).replace("-", "-"); // "08-22"
        
        for (Map.Entry<Long, Map<String, Integer>> entry : articleStatsMap.entrySet()) {
            Long articleId = entry.getKey();
            Map<String, Integer> stats = entry.getValue();
            
            try {
                for (Map.Entry<String, Integer> statEntry : stats.entrySet()) {
                    String statType = statEntry.getKey();
                    Integer count = statEntry.getValue();
                    
                    // 根据统计类型更新post_stats表中对应的字段
                    if ("view".equals(statType)) {
                        // 处理浏览量统计，现在view也通过Redis统计
                        updatePostStatsField("POST", articleId, year, dayKey, "view", count);
                    } else if ("twice".equals(statType) || "helpful".equals(statType) || "once".equals(statType)) {
                        // 处理点赞统计，支持三种点赞类型
                        updatePostStatsField("POST", articleId, year, dayKey, statType, count);
                    } else if ("comment".equals(statType)) {
                        // 处理评论统计，现在comment也通过Redis统计
                        updatePostStatsField("POST", articleId, year, dayKey, "comment", count);
                    }
                }
                updateCount++;
                log.debug("更新文章{}在{}的统计数据", articleId, dateStr);
            } catch (Exception e) {
                log.error("同步文章{}在{}的统计数据失败", articleId, dateStr, e);
            }
        }
        
        // 同步成功后删除Redis数据
        redisTemplate.delete(articleKey);
        log.info("删除Redis中{}的文章统计数据", dateStr);
        
        return updateCount;
    }

    /**
     * 更新post_stats表中的字段
     * 
     * 该方法负责将Redis中的统计数据写入数据库的post_stats表:
     * 1. 构造JSON路径，定位到具体的日期和统计类型
     * 2. 使用MySQL的JSON函数进行原子性的数值更新
     * 3. 支持增量更新，多次同步时会累加数值
     * 
     * 注意: 目前方法只记录日志，实际的数据库更新逻辑需要调用PostStatsMapper
     * 
     * @param type 对象类型，如"POST", "NODE", "ROADMAP"
     * @param objectId 对象ID
     * @param year 统计年份
     * @param dayKey 日期键，格式如"8-22"
     * @param field 统计字段，如"view", "once", "twice", "helpful", "comment"
     * @param count 统计数值
     */
    private void updatePostStatsField(String type, Long objectId, int year, String dayKey, String field, int count) {
        try {
            // TODO: 这里需要调用PostStatsMapper的相应方法来更新数据库
            // 可以参考现有的incrementUpvoteCount方法，扩展支持view和comment字段
            // postStatsMapper.incrementStatsField(type, objectId, year, dayKey, field, count);
            
            log.debug("更新post_stats: type={}, objectId={}, year={}, dayKey={}, field={}, count={}", 
                type, objectId, year, dayKey, field, count);
                
        } catch (Exception e) {
            log.error("更新post_stats失败: type={}, objectId={}, field={}, count={}", 
                type, objectId, field, count, e);
        }
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
            UserStatsDO stats = userStatsMapper.getByUserIdAndDate(userId, yesterday);
            UserStatsDTO dto = Converter.INSTANCE.toUserStatsDTO(stats);
            if (dto != null) {
                dto.setPeriod("yesterday");
                dto.setStartDate(yesterday);
                dto.setEndDate(yesterday);
            }
            return dto != null ? dto : UserStatsDTO.empty();
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
            UserStatsDO aggregatedStats = userStatsMapper.sumStatsByDateRange(userId, startDate, endDate);
            UserStatsDTO dto = Converter.INSTANCE.toUserStatsDTO(aggregatedStats);
            if (dto != null) {
                dto.setPeriod(days + "days");
                dto.setStartDate(startDate);
                dto.setEndDate(endDate);
            }
            return dto != null ? dto : UserStatsDTO.empty();
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
            List<UserStatsDO> dbStatsList = userStatsMapper.getByUserIdAndDateRange(userId, startDate, endDate);
            List<DailyStatsDTO> dailyStats = new ArrayList<>();
            
            // 总计数据
            long totalViews = 0;
            long totalTwice = 0;
            long totalHelpful = 0;
            long totalComments = 0;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                DailyStatsDTO dailyStat;
                final LocalDate currentDate = date; // 创建final变量供lambda使用
                
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
                    UserStatsDO dbStats = dbStatsList.stream()
                        .filter(s -> s.getStatDate().equals(currentDate))
                        .findFirst()
                        .orElse(null);
                        
                    if (dbStats != null) {
                        dailyStat = Converter.INSTANCE.toDailyStatsDTO(dbStats);
                    } else {
                        dailyStat = DailyStatsDTO.builder()
                            .date(date)
                            .views(0L).twice(0L).helpful(0L).comments(0L)
                            .build();
                    }
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
}