package com.prosper.learn.domain.service;

import com.prosper.learn.common.Enums.DailyStatType;
import com.prosper.learn.common.Enums.CumulativeStatType;
import com.prosper.learn.domain.service.data.UserStatsDataService;
import com.prosper.learn.domain.service.data.UserStatsYearlyDataService;
import com.prosper.learn.dto.response.UserStatsDTO;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserStatsDataService userStatsDataService;
    private final UserStatsYearlyDataService userStatsYearlyDataService;

    /**
     * 获取用户当前统计（主要接口）
     */
    public UserStatsDTO getCurrentUserStats(Long userId) {
        // 查询前确保数据完整性
        syncStaleStatsIfNeeded(userId);

        UserStatsDO currentStats = userStatsDataService.getCurrentDayStats(userId);
        if (currentStats == null) {
            // 创建今日记录
            currentStats = createTodayRecord(userId);
        }

        return convertToDTO(currentStats);
    }

    /**
     * 增量更新日度统计
     */
    @Transactional
    public void incrementDailyStat(Long userId, DailyStatType type, int delta) {
        LocalDate today = LocalDate.now();
        UserStatsDO todayStats = getOrCreateTodayRecord(userId, today);

        // 使用原子增量更新
        userStatsDataService.atomicIncrementDaily(userId, today, type.getFieldName(), delta);
    }

    /**
     * 增量更新累计统计（推荐方式）
     */
    @Transactional
    public void incrementCumulativeStat(Long userId, CumulativeStatType type, int delta) {
        LocalDate today = LocalDate.now();
        // 确保今日记录存在
        getOrCreateTodayRecord(userId, today);

        // 使用原子增量更新
        userStatsDataService.atomicIncrementCumulative(userId, today, type.getFieldName(), delta);
    }

    /**
     * 设置累计统计绝对值（用于数据修复）
     */
    @Transactional
    public void setCumulativeStat(Long userId, CumulativeStatType type, int newValue) {
        LocalDate today = LocalDate.now();
        // 确保今日记录存在
        getOrCreateTodayRecord(userId, today);

        userStatsDataService.setCumulativeStat(userId, today, type.getFieldName(), newValue);
    }

    /**
     * 批量获取用户统计（排行榜用）
     */
    public Map<Long, UserStatsDTO> batchGetUserStats(List<Long> userIds) {
        LocalDate today = LocalDate.now();
        Map<Long, UserStatsDO> statsMap = userStatsDataService.batchGetCurrentStats(userIds, today);

        // 转换为DTO
        return statsMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToDTO(entry.getValue())
                ));
    }

    /**
     * 强制刷新用户统计（管理功能）
     */
    @Transactional
    public void forceRefreshUserStats(Long userId) {
        syncStaleStatsIfNeeded(userId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取或创建今日记录
     */
    private UserStatsDO getOrCreateTodayRecord(Long userId, LocalDate date) {
        UserStatsDO todayStats = userStatsDataService.getCurrentDayStats(userId);

        if (todayStats == null) {
            // 关键：创建今日记录前检查历史数据同步
            syncStaleStatsIfNeeded(userId);
            todayStats = createTodayRecord(userId);
        }

        return todayStats;
    }

    /**
     * 创建今日记录
     */
    private UserStatsDO createTodayRecord(Long userId) {
        UserStatsDO newRecord = new UserStatsDO();
        newRecord.setUserId(userId);
        newRecord.setDailyStatDate(LocalDate.now());

        // 初始化所有字段为0
        initializeStatsToZero(newRecord);

        // TODO: 从其他服务获取当前的累计统计值
        populateCumulativeStats(newRecord);

        userStatsDataService.insert(newRecord);
        return newRecord;
    }

    /**
     * 检查并同步过期数据
     */
    private void syncStaleStatsIfNeeded(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<UserStatsDO> staleStats = userStatsDataService.getStaleStats(userId, yesterday);

        if (!staleStats.isEmpty()) {
            log.info("发现{}条待同步数据，userId: {}", staleStats.size(), userId);
            for (UserStatsDO stats : staleStats) {
                syncToYearlyAndDelete(stats);
            }
        }
    }

    /**
     * 同步到yearly表并删除
     */
    private void syncToYearlyAndDelete(UserStatsDO stats) {
        try {
            // 1. 提取日度增量数据，转换为数组格式
            int[] dailyStats = extractDailyStatsArray(stats);

            // 2. 更新yearly表的JSON（数组格式）
            userStatsYearlyDataService.updateYearlyStatsArray(
                    stats.getUserId(),
                    stats.getDailyStatDate(),
                    dailyStats
            );

            // 3. 删除已同步的数据
            userStatsDataService.deleteById(stats.getId());

            log.debug("同步完成，userId: {}, date: {}", stats.getUserId(), stats.getDailyStatDate());
        } catch (Exception e) {
            log.error("同步失败，userId: {}, date: {}",
                    stats.getUserId(), stats.getDailyStatDate(), e);
        }
    }

    /**
     * 提取日度统计数据为数组
     */
    private int[] extractDailyStatsArray(UserStatsDO stats) {
        // 转换为固定顺序的数组：[views, twice, helpful, comments]
        return new int[] {
            stats.getDailyViews() != null ? stats.getDailyViews() : 0,
            stats.getDailyTwice() != null ? stats.getDailyTwice() : 0,
            stats.getDailyHelpful() != null ? stats.getDailyHelpful() : 0,
            stats.getDailyComments() != null ? stats.getDailyComments() : 0
        };
    }

    /**
     * 初始化统计字段为0
     */
    private void initializeStatsToZero(UserStatsDO record) {
        // 日度统计初始化为0
        record.setDailyViews(0);
        record.setDailyTwice(0);
        record.setDailyHelpful(0);
        record.setDailyComments(0);

        // 累计统计初始化为0（后面会重新计算）
        record.setLearningCourses(0);
        record.setCompletedCourses(0);
        record.setInProgressProfessions(0);
        record.setCompletedProfessions(0);
        record.setFollowingUsers(0);
        record.setFollowingCourses(0);
        record.setFollowingProfessions(0);
        record.setCreatedArticles(0);
        record.setCreatedIndexs(0);
        record.setCreatedRoadmaps(0);
        record.setCreatedCardDecks(0);
    }

    /**
     * 填充累计统计数据（从其他服务获取）
     */
    private void populateCumulativeStats(UserStatsDO record) {
        Long userId = record.getUserId();

        // TODO: 从相应的服务获取累计统计
        // record.setLearningCourses(courseService.countLearningCourses(userId));
        // record.setCompletedCourses(courseService.countCompletedCourses(userId));
        // record.setFollowingUsers(followService.countFollowingUsers(userId));
        // record.setCreatedArticles(postService.countCreatedArticles(userId));
        // ... 其他统计项

        log.debug("累计统计数据填充完成，userId: {}", userId);
    }

    /**
     * 转换为DTO
     */
    private UserStatsDTO convertToDTO(UserStatsDO statsDO) {
        if (statsDO == null) {
            return UserStatsDTO.builder()
                    .dailyViews(0)
                    .dailyTwice(0)
                    .dailyHelpful(0)
                    .dailyComments(0)
                    .build();
        }

        return UserStatsDTO.builder()
                .userId(statsDO.getUserId())
                .statDate(statsDO.getDailyStatDate())
                .dailyViews(statsDO.getDailyViews())
                .dailyTwice(statsDO.getDailyTwice())
                .dailyHelpful(statsDO.getDailyHelpful())
                .dailyComments(statsDO.getDailyComments())
                .learningCourses(statsDO.getLearningCourses())
                .completedCourses(statsDO.getCompletedCourses())
                .inProgressProfessions(statsDO.getInProgressProfessions())
                .completedProfessions(statsDO.getCompletedProfessions())
                .followingUsers(statsDO.getFollowingUsers())
                .followingCourses(statsDO.getFollowingCourses())
                .followingProfessions(statsDO.getFollowingProfessions())
                .createdArticles(statsDO.getCreatedArticles())
                .createdIndexs(statsDO.getCreatedIndexs())
                .createdRoadmaps(statsDO.getCreatedRoadmaps())
                .createdCardDecks(statsDO.getCreatedCardDecks())
                .lastUpdated(statsDO.getUpdatedAt())
                .build();
    }
}