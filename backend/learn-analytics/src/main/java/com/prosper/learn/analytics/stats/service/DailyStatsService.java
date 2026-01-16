package com.prosper.learn.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.dto.DailyStatsDTO;
import com.prosper.learn.analytics.dto.UserDailyStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsWithDailyDTO;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyMapper;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
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

    // 新增：注入拆分后的同步服务
    private final UserStatsSyncService userStatsSyncService;
    private final ContentStatsSyncService contentStatsSyncService;

    /**
     * 同步昨天的统计数据
     *
     * 主要同步任务，通常在每日凌晨执行:
     * 1. 从Redis读取昨天的用户和文章统计数据
     * 2. 将数据写入数据库对应的统计表
     * 3. 删除Redis中已同步的数据以节省内存
     */
    public void syncYesterdayStats() {
        LocalDate yesterday = TimeZoneUtil.yesterday();
        String dateStr = yesterday.toString();

        log.info("开始同步{}的统计数据", dateStr);

        try {
            // 同步用户统计（使用UserStatsSyncService）
            int userStatsCount = userStatsSyncService.syncUserStats(dateStr);

            // 同步文章统计（使用ContentStatsSyncService）
            int postStatsCount = contentStatsSyncService.syncPostStats(dateStr);

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
            date = TimeZoneUtil.now();
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
                // 使用UserStatsSyncService
                userStatsCount = userStatsSyncService.syncUserStats(dateStr);
                log.info("同步{}的用户数据: {}条", dateStr, userStatsCount);
            }

            if (hasPostData) {
                // 使用ContentStatsSyncService
                postStatsCount = contentStatsSyncService.syncPostStats(dateStr);
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
    public UserDailyStatsDTO getUserTodayStats(long userId) {
        validateUserId(userId);
        String today = TimeZoneUtil.todayString();
        String userKey = generateUserStatsKey(today);

        String uKey = generatePostStatsKey(today);
        Map<Object, Object> s2tats = redisTemplate.opsForHash().entries(uKey);

        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(userKey);
            return parseUserDailyStatsFromRedis(userId, stats);
        } catch (Exception e) {
            log.error("获取用户{}今日统计失败", userId, e);
            return UserDailyStatsDTO.empty();
        }
    }

    /**
     * 获取用户历史统计（包含总计和每日明细）
     *
     * 缓存策略：按用户ID+天数+当前日期缓存，确保不同时间窗口的历史数据独立
     *
     * @param userId 用户ID
     * @param days 统计天数
     * @return 统计数据（包含总计和每日明细）
     */
    @Cacheable(value = "historyStats",
               key = "'user:' + #userId + ':' + #days + ':' + T(java.time.LocalDate).now().toString()",
               unless = "#result == null")
    public UserStatsWithDailyDTO getUserHistoryStats(long userId, int days) {
        validateUserId(userId);
        validateDaysRange(days);

        LocalDate endDate = TimeZoneUtil.now(); // 包含今天
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

                if (TimeZoneUtil.isToday(date)) {
                    // 今日数据从Redis获取
                    UserDailyStatsDTO todayStats = getUserTodayStats(userId);
                    dailyStat = DailyStatsDTO.builder()
                        .date(date.toString())
                        .viewCount(todayStats.getViewCount())
                        .twiceCount(todayStats.getTwiceCount())
                        .likeCount(todayStats.getLikeCount())
                        .commentCount(todayStats.getCommentCount())
                        .build();
                } else {
                    // 历史数据从数据库获取
                    Map<String, Integer> dayStats = getUserDayStats(userId, date);

                    dailyStat = DailyStatsDTO.builder()
                        .date(date.toString())
                        .viewCount(dayStats.get("viewCount"))
                        .twiceCount(dayStats.get("twiceCount"))
                        .likeCount(dayStats.get("likeCount"))
                        .commentCount(dayStats.get("commentCount"))
                        .build();
                }

                dailyStats.add(dailyStat);

                // 累计总数
                totalViews += dailyStat.getViewCount();
                totalTwice += dailyStat.getTwiceCount();
                totalLikes += dailyStat.getLikeCount();
                totalComments += dailyStat.getCommentCount();
            }

            return UserStatsWithDailyDTO.builder()
                .userId(userId)
                .viewCount(totalViews)
                .twiceCount(totalTwice)
                .likeCount(totalLikes)
                .commentCount(totalComments)
                .dailyStats(dailyStats)
                .build();

        } catch (Exception e) {
            log.error("获取用户{}历史统计失败: days={}", userId, days, e);
            return UserStatsWithDailyDTO.empty();
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
            .viewCount(totalViews)
            .twiceCount(totalTwice)
            .likeCount(totalLikes)
            .commentCount(totalComments)
            .build();
    }

    /**
     * 从Redis解析用户当日统计数据
     */
    private UserDailyStatsDTO parseUserDailyStatsFromRedis(long userId, Map<Object, Object> stats) {
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

        return UserDailyStatsDTO.builder()
            .userId(userId)
            .viewCount(totalViews)
            .twiceCount(totalTwice)
            .likeCount(totalLikes)
            .commentCount(totalComments)
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
                        statsMap.put("viewCount", statsArray.get(0));
                        statsMap.put("twiceCount", statsArray.get(1));
                        statsMap.put("likeCount", statsArray.get(2));
                        statsMap.put("commentCount", statsArray.get(3));
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
                    dayStats.put("viewCount", statsArray.get(0));
                    dayStats.put("twiceCount", statsArray.get(1));
                    dayStats.put("likeCount", statsArray.get(2));
                    dayStats.put("commentCount", statsArray.get(3));
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
            UserDailyStatsDTO todayStats = getUserTodayStats(userId);

            // 3. 合并数据
            int totalViews = (historicalStats.getViewCount() != null ? historicalStats.getViewCount() : 0) +
                            (todayStats.getViewCount() != null ? todayStats.getViewCount() : 0);
            int totalTwices = (historicalStats.getTwiceCount() != null ? historicalStats.getTwiceCount() : 0) +
                             (todayStats.getTwiceCount() != null ? todayStats.getTwiceCount() : 0);
            int totalLikes = (historicalStats.getLikeCount() != null ? historicalStats.getLikeCount() : 0) +
                            (todayStats.getLikeCount() != null ? todayStats.getLikeCount() : 0);
            int totalComments = (historicalStats.getCommentCount() != null ? historicalStats.getCommentCount() : 0) +
                               (todayStats.getCommentCount() != null ? todayStats.getCommentCount() : 0);

            return UserStatsDTO.builder()
                .userId(userId)
                .viewCount(totalViews)
                .twiceCount(totalTwices)
                .likeCount(totalLikes)
                .commentCount(totalComments)
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
            LocalDate yesterday = TimeZoneUtil.yesterday();
            LocalDate startDate = LocalDate.of(2020, 1, 1); // 系统开始日期

            // 使用现有的日期范围统计方法
            Map<String, Integer> totalStats = getUserDateRangeStats(userId, startDate, yesterday);

            return UserStatsDTO.builder()
                .userId(userId)
                .viewCount(totalStats.get("viewCount"))
                .twiceCount(totalStats.get("twiceCount"))
                .likeCount(totalStats.get("likeCount"))
                .commentCount(totalStats.get("commentCount"))
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
        empty.put("viewCount", 0);
        empty.put("twiceCount", 0);
        empty.put("likeCount", 0);
        empty.put("commentCount", 0);
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
        if (date.isBefore(systemStart) || date.isAfter(TimeZoneUtil.now())) {
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
                int baseViews = baseStats != null && baseStats.getViewCount() != null ? baseStats.getViewCount() : 0;
                int baseTwice = baseStats != null && baseStats.getTwiceCount() != null ? baseStats.getTwiceCount() : 0;
                int baseLikes = baseStats != null && baseStats.getLikeCount() != null ? baseStats.getLikeCount() : 0;
                int baseComments = baseStats != null && baseStats.getCommentCount() != null ? baseStats.getCommentCount() : 0;

                int todayViews = todayStats != null && todayStats.getViewCount() != null ? todayStats.getViewCount() : 0;
                int todayTwice = todayStats != null && todayStats.getTwiceCount() != null ? todayStats.getTwiceCount() : 0;
                int todayLikes = todayStats != null && todayStats.getLikeCount() != null ? todayStats.getLikeCount() : 0;
                int todayComments = todayStats != null && todayStats.getCommentCount() != null ? todayStats.getCommentCount() : 0;

                // 非按日统计字段：直接从 content_stats 获取（不加今日增量）
                int baseShares = baseStats != null && baseStats.getShareCount() != null ? baseStats.getShareCount() : 0;
                int baseBookmarks = baseStats != null && baseStats.getBookmarkCount() != null ? baseStats.getBookmarkCount() : 0;
                int baseCompleted = baseStats != null && baseStats.getCompletedUserCount() != null ? baseStats.getCompletedUserCount() : 0;
                int baseInProgress = baseStats != null && baseStats.getLearnerCount() != null ? baseStats.getLearnerCount() : 0;

                // 构建 DTO
                ContentStatsDTO dto = ContentStatsDTO.builder()
                    .contentId(contentId)
                    .viewCount(baseViews + todayViews)              // 累计 + 今日增量
                    .twiceCount(baseTwice + todayTwice)       // 累计 + 今日增量
                    .likeCount(baseLikes + todayLikes)        // 累计 + 今日增量
                    .commentCount(baseComments + todayComments)     // 累计 + 今日增量
                    .shareCount(baseShares)                         // 只用累计值
                    .bookmarkCount(baseBookmarks)                   // 只用累计值
                    .completedUserCount(baseCompleted)              // 只用累计值
                    .inProgressUserCount(baseInProgress)            // 只用累计值
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
     * 使用 HMGET 批量获取，避免N+1查询问题
     *
     * @param contentType 内容类型
     * @param contentIds 内容ID列表
     * @return Map<ContentId, DailyStatsDTO>
     */
    private Map<Long, DailyStatsDTO> batchGetTodayStatsForContent(ContentType contentType, List<Long> contentIds) {
        Map<Long, DailyStatsDTO> result = new HashMap<>();

        if (contentIds == null || contentIds.isEmpty()) {
            return result;
        }

        String today = TimeZoneUtil.todayString();
        String redisKey = STATS_KEY_PREFIX + today + CONTENT_STATS_SUFFIX;

        // 检查 Redis key 是否存在
        Boolean keyExists = redisTemplate.hasKey(redisKey);
        if (keyExists == null || !keyExists) {
            log.debug("今日 Redis 统计数据不存在: {}", redisKey);
            return result;
        }

        int contentTypeValue = contentType.value();

        // 构建所有需要查询的字段名列表
        // 对于N个内容，需要查询 4*N 个字段（views, twice, likes, comments）
        List<Object> fields = new ArrayList<>(contentIds.size() * 4);
        for (Long contentId : contentIds) {
            // Redis 字段格式：contentType:contentId:statType
            // 例如：1:123:view (ContentType.post=1, contentId=123, statType=view)
            fields.add(contentTypeValue + ":" + contentId + ":" + STAT_TYPE_VIEW);
            fields.add(contentTypeValue + ":" + contentId + ":" + STAT_TYPE_TWICE);
            fields.add(contentTypeValue + ":" + contentId + ":" + STAT_TYPE_LIKE);
            fields.add(contentTypeValue + ":" + contentId + ":" + STAT_TYPE_COMMENT);
        }

        try {
            // 使用 HMGET 批量获取所有字段值，只需要一次 Redis 调用
            List<Object> values = redisTemplate.opsForHash().multiGet(redisKey, fields);

            // 解析返回的结果，每4个值对应一个内容的统计
            for (int i = 0; i < contentIds.size(); i++) {
                Long contentId = contentIds.get(i);
                int baseIndex = i * 4;

                // 解析四个统计字段（按照构建fields时的顺序）
                int views = parseRedisValue(values.get(baseIndex));
                int twice = parseRedisValue(values.get(baseIndex + 1));
                int likes = parseRedisValue(values.get(baseIndex + 2));
                int comments = parseRedisValue(values.get(baseIndex + 3));

                DailyStatsDTO stats = DailyStatsDTO.builder()
                    .viewCount(views)
                    .twiceCount(twice)
                    .likeCount(likes)
                    .commentCount(comments)
                    .build();

                result.put(contentId, stats);
            }

            log.debug("批量获取{}个内容的今日统计，使用HMGET一次查询完成", contentIds.size());

        } catch (Exception e) {
            log.error("批量获取今日统计失败，回退到逐个查询: contentType={}, contentIds.size={}",
                contentType, contentIds.size(), e);
            // 降级：如果批量查询失败，回退到逐个查询
            return batchGetTodayStatsForContentFallback(contentType, contentIds, redisKey, contentTypeValue);
        }

        return result;
    }

    /**
     * 解析Redis返回值为整数
     *
     * @param value Redis返回的值，可能为null或字符串
     * @return 解析后的整数值，如果为null或解析失败则返回0
     */
    private int parseRedisValue(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.debug("解析Redis值失败: value={}", value);
            return 0;
        }
    }

    /**
     * 批量获取今日统计的降级方法（逐个查询）
     *
     * 当批量查询失败时使用此方法作为后备方案
     */
    private Map<Long, DailyStatsDTO> batchGetTodayStatsForContentFallback(
            ContentType contentType, List<Long> contentIds, String redisKey, int contentTypeValue) {

        Map<Long, DailyStatsDTO> result = new HashMap<>();

        for (Long contentId : contentIds) {
            try {
                int views = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_VIEW);
                int twice = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_TWICE);
                int likes = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_LIKE);
                int comments = getRedisHashFieldAsInt(redisKey, contentTypeValue + ":" + contentId + ":" + STAT_TYPE_COMMENT);

                DailyStatsDTO stats = DailyStatsDTO.builder()
                    .viewCount(views)
                    .twiceCount(twice)
                    .likeCount(likes)
                    .commentCount(comments)
                    .build();

                result.put(contentId, stats);
            } catch (Exception e) {
                log.warn("获取内容{}的今日统计失败", contentId, e);
            }
        }

        return result;
    }
}
