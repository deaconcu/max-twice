package com.prosper.learn.business.service.data;

import com.prosper.learn.persistence.dataobject.UserStatsDO;
import com.prosper.learn.persistence.mapper.UserStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户统计数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsDataService {

    private final UserStatsMapper userStatsMapper;

    /**
     * 获取用户当日统计
     */
    public UserStatsDO getCurrentDayStats(Long userId) {
        LocalDate today = LocalDate.now();
        return userStatsMapper.getByUserIdAndDate(userId, today);
    }

    /**
     * 日度统计原子增量更新
     */
    public int atomicIncrementDaily(Long userId, LocalDate date, String field, int delta) {
        return userStatsMapper.atomicIncrementDaily(userId, date, field, delta);
    }

    /**
     * 累计统计原子增量更新（推荐方式）
     */
    public int atomicIncrementCumulative(Long userId, LocalDate date, String field, int delta) {
        return userStatsMapper.atomicIncrementCumulative(userId, date, field, delta);
    }

    /**
     * 累计统计设置绝对值（数据修复用）
     */
    public int setCumulativeStat(Long userId, LocalDate date, String field, int newValue) {
        return userStatsMapper.setCumulativeStat(userId, date, field, newValue);
    }

    /**
     * 批量获取用户统计
     */
    public Map<Long, UserStatsDO> batchGetCurrentStats(List<Long> userIds, LocalDate date) {
        List<UserStatsDO> statsList = userStatsMapper.batchGetCurrentStats(userIds, date);
        return statsList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserStatsDO::getUserId,
                        stats -> stats
                ));
    }

    /**
     * 获取待同步的历史数据
     */
    public List<UserStatsDO> getStaleStats(Long userId, LocalDate beforeDate) {
        return userStatsMapper.getStaleStats(userId, beforeDate);
    }

    /**
     * 获取排行榜数据
     */
    public List<UserStatsDO> getTopUsersByField(String field, int limit, LocalDate date) {
        return userStatsMapper.getTopUsersByField(field, limit, date);
    }

    /**
     * 插入新记录
     */
    public int insert(UserStatsDO userStats) {
        return userStatsMapper.insert(userStats);
    }

    /**
     * 根据ID删除
     */
    public int deleteById(Long id) {
        return userStatsMapper.deleteById(id);
    }

    /**
     * 获取超过指定天数的老数据
     */
    public List<UserStatsDO> getStatsOlderThan(LocalDate currentDate, int days) {
        return userStatsMapper.getStatsOlderThan(currentDate, days);
    }
}