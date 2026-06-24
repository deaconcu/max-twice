package com.twicemax.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.analytics.dto.DailyStatsDTO;
import com.twicemax.analytics.dto.UserDailyStatsDTO;
import com.twicemax.analytics.dto.UserStatsWithDailyDTO;
import com.twicemax.analytics.stats.dataservice.UserStatsDataService;
import com.twicemax.analytics.stats.mapper.UserStatsDO;
import com.twicemax.analytics.stats.mapper.UserStatsYearlyDO;
import com.twicemax.analytics.stats.mapper.UserStatsYearlyMapper;
import com.twicemax.shared.common.util.TimeZoneUtil;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapApprovedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapBannedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.RoadmapRestoredEvent;
import com.twicemax.shared.domain.exception.BusinessException;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
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

import static com.twicemax.shared.domain.Enums.ContentState;
import static com.twicemax.shared.domain.Enums.PostType;

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
    public UserStatsDO getUserStats(Long userId) {
        return userStatsDataService.getOrCreate(userId);
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
     * 增量更新关注角色数
     */
    @Transactional
    public void incrementFollowingRoles(Long userId, int delta) {
        userStatsDataService.incrementFollowingRoles(userId, delta);
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

    // ==================== 记忆卡片复习统计 ====================

    /**
     * 更新用户的记忆卡片复习连续天数
     *
     * @param userId 用户ID
     * @param userToday 用户时区的今天日期（由调用方根据用户时区计算）
     */
    @Transactional
    public void updateReviewStreak(Long userId, LocalDate userToday) {
        UserStatsDO stats = userStatsDataService.getOrCreate(userId);

        LocalDate lastDate = stats.getLastCardReviewDate();
        int newStreakDays;

        if (lastDate == null) {
            // 首次复习
            newStreakDays = 1;
        } else if (lastDate.equals(userToday)) {
            // 今天已经复习过，不更新
            return;
        } else if (lastDate.equals(userToday.minusDays(1))) {
            // 昨天复习过，连续 +1
            newStreakDays = (stats.getReviewStreakDays() != null ? stats.getReviewStreakDays() : 0) + 1;
        } else {
            // 断了，重新从 1 开始
            newStreakDays = 1;
        }

        userStatsDataService.updateReviewStreak(userId, newStreakDays, userToday);
    }

    /**
     * 获取用户的记忆卡片复习连续天数
     *
     * @param userId 用户ID
     * @param userToday 用户时区的今天日期（由调用方根据用户时区计算）
     * @return 有效的连续复习天数
     */
    public int getReviewStreakDays(Long userId, LocalDate userToday) {
        UserStatsDO stats = userStatsDataService.getByUserId(userId);

        if (stats == null || stats.getLastCardReviewDate() == null) {
            return 0;
        }

        LocalDate lastDate = stats.getLastCardReviewDate();

        // 今天或昨天有复习，streak 有效
        if (lastDate.equals(userToday) || lastDate.equals(userToday.minusDays(1))) {
            return stats.getReviewStreakDays() != null ? stats.getReviewStreakDays() : 0;
        }

        // 超过一天没复习，streak 失效
        return 0;
    }

    // ==================== 学习统计（阅读文章）====================

    /**
     * 更新用户的连续学习天数
     *
     * @param userId 用户ID
     * @param userToday 用户时区的今天日期（由调用方根据用户时区计算）
     */
    @Transactional
    public void updateLearningStreak(Long userId, LocalDate userToday) {
        UserStatsDO stats = userStatsDataService.getOrCreate(userId);

        LocalDate lastDate = stats.getLastLearningDate();
        int newStreakDays;

        if (lastDate == null) {
            // 首次学习
            newStreakDays = 1;
        } else if (lastDate.equals(userToday)) {
            // 今天已经学习过，不更新
            return;
        } else if (lastDate.equals(userToday.minusDays(1))) {
            // 昨天学习过，连续 +1
            newStreakDays = (stats.getLearningStreakDays() != null ? stats.getLearningStreakDays() : 0) + 1;
        } else {
            // 断了，重新从 1 开始
            newStreakDays = 1;
        }

        userStatsDataService.updateLearningStreak(userId, newStreakDays, userToday);
    }

    /**
     * 获取用户的连续学习天数
     *
     * @param userId 用户ID
     * @param userToday 用户时区的今天日期（由调用方根据用户时区计算）
     * @return 有效的连续学习天数
     */
    public int getLearningStreakDays(Long userId, LocalDate userToday) {
        UserStatsDO stats = userStatsDataService.getByUserId(userId);

        if (stats == null || stats.getLastLearningDate() == null) {
            return 0;
        }

        LocalDate lastDate = stats.getLastLearningDate();

        // 今天或昨天有学习，streak 有效
        if (lastDate.equals(userToday) || lastDate.equals(userToday.minusDays(1))) {
            return stats.getLearningStreakDays() != null ? stats.getLearningStreakDays() : 0;
        }

        // 超过一天没学习，streak 失效
        return 0;
    }

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
    public Map<Long, UserStatsDO> batchGetUserStats(List<Long> userIds) {
        return userStatsDataService.batchGetByUserIds(userIds);
    }

    /**
     * 获取排行榜 Top N 用户
     */
    public List<UserStatsDO> getTopUsersByField(String field, int limit) {
        return switch (field) {
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
    }

    // ==================== 事件监听：用户维度统计更新 ====================

    /**
     * 监听内容审核通过事件 - 增加用户创作统计
     */
    @EventListener
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostApproved(event);
                case roadmap -> { /* 走独立 RoadmapApprovedEvent */ }
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
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostRemoved(event);
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
    public void onContentBanned(ContentBannedEvent event) {
        try {
            if (event.getPreviousState() != ContentState.PUBLISHED) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostBanned(event);
                case roadmap -> { /* 走独立 RoadmapBannedEvent */ }
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
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            if (event.getPreviousState() != ContentState.BANNED) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostRestored(event);
                case roadmap -> { /* 走独立 RoadmapRestoredEvent */ }
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), 1);
            }
        } catch (Exception e) {
            log.error("处理内容恢复事件失败(用户统计): {}", e.getMessage());
        }
    }

    // ==================== Roadmap revision 模型独立事件 ====================

    @EventListener
    public void onRoadmapApproved(RoadmapApprovedEvent event) {
        try {
            userStatsDataService.incrementCreatedRoadmaps(event.getAuthorId(), 1);
            log.debug("Roadmap 审核通过，用户创建路线图统计++: authorId={}", event.getAuthorId());
        } catch (Exception e) {
            log.error("处理 Roadmap 审核通过事件失败(用户统计): {}", e.getMessage());
        }
    }

    @EventListener
    public void onRoadmapBanned(RoadmapBannedEvent event) {
        try {
            // 仅 PUBLISHED 被 ban 才需要回滚（NEVER_PUBLISHED 没计入过）
            if (event.getPreviousState() != NewContentState.PUBLISHED) return;
            userStatsDataService.incrementCreatedRoadmaps(event.getAuthorId(), -1);
            log.debug("Roadmap 封禁，用户创建路线图统计--: authorId={}", event.getAuthorId());
        } catch (Exception e) {
            log.error("处理 Roadmap 封禁事件失败(用户统计): {}", e.getMessage());
        }
    }

    @EventListener
    public void onRoadmapRestored(RoadmapRestoredEvent event) {
        try {
            // 仅恢复成 PUBLISHED 才计入
            if (event.getNewState() != NewContentState.PUBLISHED) return;
            userStatsDataService.incrementCreatedRoadmaps(event.getAuthorId(), 1);
            log.debug("Roadmap 恢复发布，用户创建路线图统计++: authorId={}", event.getAuthorId());
        } catch (Exception e) {
            log.error("处理 Roadmap 恢复事件失败(用户统计): {}", e.getMessage());
        }
    }

    // ==================== Post 处理 ====================

    private void handlePostApproved(ContentApprovedEvent event) {
        if (event.getPostType() == PostType.INDEX) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        } else if (event.getPostType() == PostType.ARTICLE) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
        }
    }

    private void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getPostType() == PostType.INDEX) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        } else if (event.getPostType() == PostType.ARTICLE) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        }
    }

    private void handlePostBanned(ContentBannedEvent event) {
        if (event.getPostType() == PostType.INDEX) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        } else if (event.getPostType() == PostType.ARTICLE) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        }
    }

    private void handlePostRestored(ContentRestoredEvent event) {
        if (event.getPostType() == PostType.INDEX) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        } else if (event.getPostType() == PostType.ARTICLE) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
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
     * 获取用户全部时间统计（累计数据 + 今日实时数据）
     *
     * @param userId 用户ID
     * @return 全部时间的统计数据（DO对象，合并了今日实时数据）
     */
    public UserStatsDO getUserAllTimeStats(long userId) {
        // 1. 从 user_stats 表获取累计数据
        UserStatsDO statsDO = userStatsDataService.getOrCreate(userId);

        // 2. 获取今天的实时数据
        UserDailyStatsDTO todayStats = redisStatsDomainService.getUserTodayStats(userId);

        // 3. 合并今日数据到累计数据
        statsDO.setViewCount((statsDO.getViewCount() != null ? statsDO.getViewCount() : 0) +
                           (todayStats.getViewCount() != null ? todayStats.getViewCount() : 0));
        statsDO.setTwiceCount((statsDO.getTwiceCount() != null ? statsDO.getTwiceCount() : 0) +
                            (todayStats.getTwiceCount() != null ? todayStats.getTwiceCount() : 0));
        statsDO.setLikeCount((statsDO.getLikeCount() != null ? statsDO.getLikeCount() : 0) +
                           (todayStats.getLikeCount() != null ? todayStats.getLikeCount() : 0));
        statsDO.setCommentCount((statsDO.getCommentCount() != null ? statsDO.getCommentCount() : 0) +
                              (todayStats.getCommentCount() != null ? todayStats.getCommentCount() : 0));

        return statsDO;
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
            if (e instanceof BusinessException) {
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