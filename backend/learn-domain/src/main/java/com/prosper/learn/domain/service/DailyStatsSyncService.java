package com.prosper.learn.domain.service;

import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.DailyStatsDTO;
import com.prosper.learn.dto.UserStatsDTO;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
import com.prosper.learn.persistence.mapper.UpvoteStatsMapper;
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

@Slf4j
@Service
public class DailyStatsSyncService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserStatsMapper userStatsMapper;

    @Autowired
    private UpvoteStatsMapper upvoteStatsMapper;

    /**
     * 同步昨天的统计数据
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
     */
    private int syncUserStats(String dateStr) {
        String userKey = "stats:" + dateStr + ":user";
        Map<Object, Object> userStats = redisTemplate.opsForHash().entries(userKey);
        
        if (userStats.isEmpty()) {
            log.info("{}没有用户统计数据", dateStr);
            return 0;
        }
        
        // 按用户ID分组统计
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
     * 同步文章统计数据到upvote_stats表
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
                    
                    // 更新upvote_stats表中的数据
                    if ("view".equals(statType)) {
                        // 扩展upvote_stats支持view字段
                        updateUpvoteStatsField("POST", articleId, year, dayKey, "view", count);
                    } else if ("twice".equals(statType) || "helpful".equals(statType)) {
                        updateUpvoteStatsField("POST", articleId, year, dayKey, statType, count);
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
     * 更新upvote_stats表中的字段
     */
    private void updateUpvoteStatsField(String type, Long objectId, int year, String dayKey, String field, int count) {
        try {
            // 这里需要扩展UpvoteStatsMapper，添加支持view等新字段的更新方法
            log.debug("更新upvote_stats: type={}, objectId={}, year={}, dayKey={}, field={}, count={}", 
                type, objectId, year, dayKey, field, count);
                
        } catch (Exception e) {
            log.error("更新upvote_stats失败: type={}, objectId={}, field={}, count={}", 
                type, objectId, field, count, e);
        }
    }

    // ====== 查询服务方法 ======

    /**
     * 获取用户今日统计（从Redis）
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
     */
    private UserStatsDTO parseUserStatsFromRedis(Integer userId, Map<Object, Object> stats, String date) {
        long totalViews = 0;
        long totalTwice = 0;
        long totalHelpful = 0;
        long totalComments = 0;
        
        String userPrefix = userId + ":";
        
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