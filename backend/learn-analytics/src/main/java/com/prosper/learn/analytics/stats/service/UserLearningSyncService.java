package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.stats.mapper.UserLearningDailyDO;
import com.prosper.learn.analytics.stats.mapper.UserLearningDailyMapper;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户学习统计同步服务
 *
 * 负责将 user_learning_daily 表中的数据同步到 user_stats_yearly 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLearningSyncService {

    private final UserLearningDailyMapper userLearningDailyMapper;
    private final UserStatsYearlyMapper userStatsYearlyMapper;

    /**
     * 同步昨天的学习统计数据
     *
     * 将 user_learning_daily 表中昨天的数据同步到 user_stats_yearly 表
     *
     * @return 同步的记录数
     */
    public int syncYesterdayLearningStats() {
        LocalDate yesterday = TimeZoneUtil.yesterday();
        return syncLearningStats(yesterday);
    }

    /**
     * 同步指定日期的学习统计数据
     *
     * @param date 要同步的日期
     * @return 同步的记录数
     */
    public int syncLearningStats(LocalDate date) {
        log.info("开始同步{}的学习统计数据", date);

        try {
            // 1. 获取指定日期的所有学习数据
            List<UserLearningDailyDO> dailyStats = userLearningDailyMapper.getAllByDate(date);

            if (dailyStats.isEmpty()) {
                log.info("{}没有学习统计数据需要同步", date);
                return 0;
            }

            int year = date.getYear();
            String dayKey = generateDayKey(date);
            int syncCount = 0;

            // 2. 逐条同步到 yearly 表
            for (UserLearningDailyDO daily : dailyStats) {
                try {
                    // 确保用户的年度记录存在
                    ensureUserYearRecord(daily.getUserId(), year);

                    // 更新学习统计数据
                    int updated = userStatsYearlyMapper.updateYearlyLearningStats(
                        daily.getUserId(),
                        year,
                        dayKey,
                        daily.getCompletedNodes() != null ? daily.getCompletedNodes() : 0,
                        daily.getCancelCompletedNodes() != null ? daily.getCancelCompletedNodes() : 0,
                        daily.getReviewedCards() != null ? daily.getReviewedCards() : 0
                    );

                    if (updated > 0) {
                        syncCount++;
                        log.debug("同步用户{}在{}的学习数据: completedNodes={}, reviewedCards={}, cancelCompletedNodes={}",
                            daily.getUserId(), date, daily.getCompletedNodes(), daily.getReviewedCards(), daily.getCancelCompletedNodes());
                    }
                } catch (Exception e) {
                    log.error("同步用户{}在{}的学习数据失败", daily.getUserId(), date, e);
                }
            }

            // 3. 删除已同步的 daily 数据（只删除非今天的数据）
            LocalDate today = TimeZoneUtil.now();
            if (!date.equals(today)) {
                int deleted = userLearningDailyMapper.deleteByDate(date);
                log.info("删除{}的daily学习数据: {}条", date, deleted);
            }

            log.info("同步{}的学习统计数据完成: 共{}条", date, syncCount);
            return syncCount;

        } catch (Exception e) {
            log.error("同步{}的学习统计数据失败", date, e);
            throw new RuntimeException("同步学习统计数据失败", e);
        }
    }

    /**
     * 清理过期的 daily 数据（保留最近 N 天）
     *
     * @param keepDays 保留天数
     * @return 删除的记录数
     */
    public int cleanupOldDailyData(int keepDays) {
        LocalDate cutoffDate = TimeZoneUtil.now().minusDays(keepDays);
        int deleted = userLearningDailyMapper.deleteBeforeDate(cutoffDate);
        log.info("清理{}之前的daily学习数据: {}条", cutoffDate, deleted);
        return deleted;
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
            yearRecord.setStats("{}");
            userStatsYearlyMapper.insert(yearRecord);
            log.debug("创建用户{}的{}年度统计记录", userId, year);
        }
    }

    /**
     * 生成日期键（月-日格式）
     */
    private String generateDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }
}
