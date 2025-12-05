package com.prosper.learn.analytics.stats.dataservice;

import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.shared.dataservice.AbstractDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户年度统计数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsYearlyDataService extends AbstractDataService<UserStatsYearlyDO, UserStatsYearlyMapper, Long> {

    private final UserStatsYearlyMapper userStatsYearlyMapper;

    /**
     * 更新yearly表的JSON统计数据
     */
    public void updateYearlyStatsArray(Long userId, LocalDate date, int[] dailyStats) {
        int statYear = date.getYear();
        String dateKey = date.getMonthValue() + "-" + date.getDayOfMonth();

        userStatsYearlyMapper.updateYearlyStatsArray(
                userId,
                statYear,
                dateKey,
                dailyStats[0], // views
                dailyStats[1], // twice
                dailyStats[2], // helpful
                dailyStats[3]  // comments
        );
    }

    /**
     * 获取用户历史统计数据
     */
    public List<UserStatsYearlyDO> getUserHistoryStats(Long userId, int startYear) {
        return userStatsYearlyMapper.getStatsInYearRange(userId, startYear);
    }

    /**
     * 根据用户ID和年份获取统计
     */
    public UserStatsYearlyDO getByUserIdAndYear(Long userId, int statYear) {
        return userStatsYearlyMapper.getByUserIdAndYear(userId, statYear);
    }

    @Override
    protected UserStatsYearlyMapper getMapper() {
        return userStatsYearlyMapper;
    }

    @Override
    protected String getCacheKeyPrefix() {
        return "user_stats_yearly";
    }

    @Override
    protected Class<UserStatsYearlyDO> getEntityClass() {
        return UserStatsYearlyDO.class;
    }
}