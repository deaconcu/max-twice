package com.prosper.learn.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.dto.DailyStatsDTO;
import com.prosper.learn.analytics.dto.UserDailyStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.dto.UserStatsWithDailyDTO;
import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.event.content.lifecycle.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.ContentState;

/**
 * 用户统计领域服务
 *
 * 负责用户统计相关的业务逻辑，包括：
 * - 用户累计统计数据查询
 * - 统计数据增量更新
 * - 排行榜查询
 * - 监听内容审核事件，更新用户维度的统计（user_stats表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsDomainService {

    private final UserStatsDataService userStatsDataService;
    private final UserStatsYearlyMapper userStatsYearlyMapper;
    private final RedisStatsDomainService redisStatsDomainService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;

    /**
     * 获取用户统计数据
     */
    public UserStatsDTO getUserStats(Long userId) {
        UserStatsDO stats = userStatsDataService.getOrCreate(userId);
        return convertToDTO(stats);
    }

    /**
     * 增量更新浏览量
     */
    @Transactional
    public void incrementViews(Long userId, int delta) {
        userStatsDataService.incrementViews(userId, delta);
    }

    /**
     * 增量更新两次能懂数
     */
    @Transactional
    public void incrementTwices(Long userId, int delta) {
        userStatsDataService.incrementTwices(userId, delta);
    }

    /**
     * 增量更新有用点赞数
     */
    @Transactional
    public void incrementLikes(Long userId, int delta) {
        userStatsDataService.incrementLikes(userId, delta);
    }

    /**
     * 增量更新评论数
     */
    @Transactional
    public void incrementComments(Long userId, int delta) {
        userStatsDataService.incrementComments(userId, delta);
    }

    /**
     * 设置字段绝对值（用于数据修复，内部使用，不暴露给外部）
     */
    @Transactional
    private void setField(Long userId, String field, int newValue) {
        // 根据field调用对应的专用方法
        switch (field) {
            case "following_users":
                userStatsDataService.setFollowingUsers(userId, newValue);
                break;
            case "following_courses":
                userStatsDataService.setFollowingCourses(userId, newValue);
                break;
            case "following_professions":
                userStatsDataService.setFollowingProfessions(userId, newValue);
                break;
            case "learning_courses":
                userStatsDataService.setLearningCourses(userId, newValue);
                break;
            case "completed_courses":
                userStatsDataService.setCompletedCourses(userId, newValue);
                break;
            case "in_progress_professions":
                userStatsDataService.setInProgressProfessions(userId, newValue);
                break;
            case "completed_professions":
                userStatsDataService.setCompletedProfessions(userId, newValue);
                break;
            default:
                log.warn("不支持的字段: {}", field);
                throw new IllegalArgumentException("不支持的字段: " + field);
        }
    }

    // ==================== 社交关系统计 ====================

    /**
     * 增量更新关注用户数
     */
    @Transactional
    public void incrementFollowingUsers(Long userId, int delta) {
        userStatsDataService.incrementFollowingUsers(userId, delta);
    }

    /**
     * 增量更新关注课程数
     */
    @Transactional
    public void incrementFollowingCourses(Long userId, int delta) {
        userStatsDataService.incrementFollowingCourses(userId, delta);
    }

    /**
     * 增量更新关注职业数
     */
    @Transactional
    public void incrementFollowingProfessions(Long userId, int delta) {
        userStatsDataService.incrementFollowingProfessions(userId, delta);
    }

    // ==================== 学习进度统计 ====================

    /**
     * 增量更新正在学习课程数
     */
    @Transactional
    public void incrementLearningCourses(Long userId, int delta) {
        userStatsDataService.incrementLearningCourses(userId, delta);
    }

    /**
     * 增量更新已完成课程数
     */
    @Transactional
    public void incrementCompletedCourses(Long userId, int delta) {
        userStatsDataService.incrementCompletedCourses(userId, delta);
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 增量更新正在进行职业数
//     */
//    @Transactional
//    public void incrementInProgressProfessions(Long userId, int delta) {
//        userStatsDataService.incrementInProgressProfessions(userId, delta);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 增量更新已完成职业数
//     */
//    @Transactional
//    public void incrementCompletedProfessions(Long userId, int delta) {
//        userStatsDataService.incrementCompletedProfessions(userId, delta);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    // ==================== 内容创作统计 ====================

    /**
     * 增量更新创建文章数
     */
    @Transactional
    public void incrementCreatedArticles(Long userId, int delta) {
        userStatsDataService.incrementCreatedArticles(userId, delta);
    }

    /**
     * 增量更新创建目录数
     */
    @Transactional
    public void incrementCreatedIndexs(Long userId, int delta) {
        userStatsDataService.incrementCreatedIndexs(userId, delta);
    }

    /**
     * 增量更新创建路线图数
     */
    @Transactional
    public void incrementCreatedRoadmaps(Long userId, int delta) {
        userStatsDataService.incrementCreatedRoadmaps(userId, delta);
    }

    /**
     * 增量更新创建卡片组数
     */
    @Transactional
    public void incrementCreatedCardDecks(Long userId, int delta) {
        userStatsDataService.incrementCreatedCardDecks(userId, delta);
    }

    // ==================== 查询方法 ====================

    /**
     * 批量获取用户统计（排行榜用）
     */
    public Map<Long, UserStatsDTO> batchGetUserStats(List<Long> userIds) {
        Map<Long, UserStatsDO> statsMap = userStatsDataService.batchGetByUserIds(userIds);

        return statsMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToDTO(entry.getValue())
                ));
    }

    /**
     * 获取排行榜 Top N 用户
     */
    public List<UserStatsDTO> getTopUsersByField(String field, int limit) {
        List<UserStatsDO> topUsers = switch (field) {
            case "views" -> userStatsDataService.getTopUsersByViews(limit);
            case "twices" -> userStatsDataService.getTopUsersByTwices(limit);
            case "likes" -> userStatsDataService.getTopUsersByLikes(limit);
            case "comments" -> userStatsDataService.getTopUsersByComments(limit);
            case "created_articles" -> userStatsDataService.getTopUsersByCreatedArticles(limit);
            case "created_indexs" -> userStatsDataService.getTopUsersByCreatedIndexs(limit);
            case "created_roadmaps" -> userStatsDataService.getTopUsersByCreatedRoadmaps(limit);
            case "created_card_decks" -> userStatsDataService.getTopUsersByCreatedCardDecks(limit);
            default -> throw new IllegalArgumentException("不支持的排序字段: " + field);
        };
        return topUsers.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 转换为DTO
     */
    private UserStatsDTO convertToDTO(UserStatsDO statsDO) {
        if (statsDO == null) {
            return UserStatsDTO.empty();
        }

        return UserStatsDTO.builder()
                .userId(statsDO.getUserId())
                .viewCount(statsDO.getViewCount())
                .twiceCount(statsDO.getTwiceCount())
                .likeCount(statsDO.getLikeCount())
                .commentCount(statsDO.getCommentCount())
                .learningCourseCount(statsDO.getLearningCourseCount())
                .completedCourseCount(statsDO.getCompletedCourseCount())
                .inProgressProfessionCount(statsDO.getInProgressProfessionCount())
                .completedProfessionCount(statsDO.getCompletedProfessionCount())
                .followingUserCount(statsDO.getFollowingUserCount())
                .followingCourseCount(statsDO.getFollowingCourseCount())
                .followingProfessionCount(statsDO.getFollowingProfessionCount())
                .createdArticleCount(statsDO.getCreatedArticleCount())
                .createdIndexCount(statsDO.getCreatedIndexCount())
                .createdRoadmapCount(statsDO.getCreatedRoadmapCount())
                .createdCardDeckCount(statsDO.getCreatedCardDeckCount())
                .lastUpdated(statsDO.getUpdatedAt())
                .build();
    }

    // ==================== 事件监听：用户维度统计更新 ====================

    /**
     * 监听内容审核通过事件 - 增加用户创作统计
     */
    @EventListener
    //@Async
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostApproved(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), 1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), 1);
            }
        } catch (Exception e) {
            log.error("处理内容审核通过事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容下架事件 - 减少用户创作统计
     */
    @EventListener
    //@Async
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostRemoved(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), -1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), -1);
            }
        } catch (Exception e) {
            log.error("处理内容下架事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容封禁事件 - 减少用户创作统计（仅 PUBLISHED 状态）
     */
    @EventListener
    //@Async
    public void onContentBanned(ContentBannedEvent event) {
        try {
            if (event.getPreviousState() != ContentState.PUBLISHED.value()) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostBanned(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), -1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), -1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), -1);
            }
        } catch (Exception e) {
            log.error("处理内容封禁事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容恢复事件 - 恢复用户创作统计
     */
    @EventListener
    //@Async
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            if (event.getPreviousState() != ContentState.BANNED.value()) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostRestored(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), 1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), 1);
            }
        } catch (Exception e) {
            log.error("处理内容恢复事件失败(用户统计): {}", e.getMessage());
        }
    }

    // ==================== Post 处理 ====================

    private void handlePostApproved(ContentApprovedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        }
    }

    private void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        }
    }

    private void handlePostBanned(ContentBannedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        }
    }

    private void handlePostRestored(ContentRestoredEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        }
    }

    // ==================== 查询方法（从 DailyStatsService 迁移）====================

    /**
     * 获取用户历史统计（包含总计和每日明细）
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

        LocalDate endDate = TimeZoneUtil.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        try {
            List<DailyStatsDTO> dailyStats = new ArrayList<>();
            int totalViews = 0;
            int totalTwice = 0;
            int totalLikes = 0;
            int totalComments = 0;

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                DailyStatsDTO dailyStat;

                if (TimeZoneUtil.isToday(date)) {
                    // 今日数据从Redis获取
                    UserDailyStatsDTO todayStats = redisStatsDomainService.getUserTodayStats(userId);
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
     * 获取用户全部时间统计（历史数据 + 今日实时数据）
     *
     * @param userId 用户ID
     * @return 全部时间的统计数据
     */
    @Cacheable(value = "allTimeStats",
               key = "'user:' + #userId + ':' + T(java.time.LocalDate).now().minusDays(1).toString()",
               unless = "#result == null")
    public UserStatsDTO getUserAllTimeStats(long userId) {
        try {
            // 1. 获取昨天以前的所有历史数据
            UserStatsDTO historicalStats = getUserHistoricalStats(userId);

            // 2. 获取今天的实时数据
            UserDailyStatsDTO todayStats = redisStatsDomainService.getUserTodayStats(userId);

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
     * @param userId 用户ID
     * @return 历史数据汇总
     */
    @Cacheable(value = "historicalStats",
               key = "'user:' + #userId + ':until:' + T(java.time.LocalDate).now().minusDays(1).toString()",
               unless = "#result == null")
    public UserStatsDTO getUserHistoricalStats(long userId) {
        try {
            LocalDate yesterday = TimeZoneUtil.yesterday();
            LocalDate startDate = LocalDate.of(2020, 1, 1);

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
     * @param userId 用户ID
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 日期范围内的汇总统计数据
     */
    public Map<String, Integer> getUserDateRangeStats(long userId, LocalDate startDate, LocalDate endDate) {
        try {
            Map<String, Integer> total = createEmptyUserStatsMap();

            int startYear = startDate.getYear();
            int endYear = endDate.getYear();

            for (int year = startYear; year <= endYear; year++) {
                Map<String, Map<String, Integer>> yearStats = getUserYearStats(userId, year);

                for (Map.Entry<String, Map<String, Integer>> entry : yearStats.entrySet()) {
                    String dayKey = entry.getKey();
                    LocalDate dayDate = parseDayKey(year, dayKey);

                    if (dayDate != null &&
                        (dayDate.isEqual(startDate) || dayDate.isAfter(startDate)) &&
                        (dayDate.isEqual(endDate) || dayDate.isBefore(endDate))) {

                        Map<String, Integer> dayStats = entry.getValue();
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
     * 获取用户指定月份的统计汇总
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

            for (Map.Entry<String, Map<String, Integer>> entry : yearStats.entrySet()) {
                String dayKey = entry.getKey();
                String[] parts = dayKey.split("-");
                if (parts.length == 2) {
                    int dayMonth = Integer.parseInt(parts[0]);
                    if (dayMonth == month) {
                        Map<String, Integer> dayStats = entry.getValue();
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
     * 获取用户指定日期的统计数据
     *
     * @param userId 用户ID
     * @param date 查询日期
     * @return 当日各项统计数据的映射
     */
    public Map<String, Integer> getUserDayStats(long userId, LocalDate date) {
        validateUserId(userId);
        validateDate(date);
        try {
            int year = date.getYear();
            String dayKey = generateDayKey(date);

            String dayStatsJson = userStatsYearlyMapper.getDayStats(userId, year, dayKey);

            if (dayStatsJson != null) {
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

            return createEmptyUserStatsMap();
        } catch (Exception e) {
            if (e instanceof com.prosper.learn.shared.domain.exception.BusinessException) {
                throw e;
            }
            log.error("获取用户日统计失败: userId={}, date={}", userId, date, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 获取用户指定年份的完整统计数据
     *
     * @param userId 用户ID
     * @param year 查询年份
     * @return 年度统计数据
     */
    public Map<String, Map<String, Integer>> getUserYearStats(long userId, int year) {
        try {
            UserStatsYearlyDO yearStats = userStatsYearlyMapper.getByUserIdAndYear(userId, year);
            if (yearStats == null || yearStats.getStats() == null) {
                return new HashMap<>();
            }

            Map<String, List<Integer>> yearStatsArray = objectMapper.readValue(yearStats.getStats(),
                new TypeReference<Map<String, List<Integer>>>() {});

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

    // ==================== 私有辅助方法 ====================

    /**
     * 创建空的用户统计数据映射
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
}