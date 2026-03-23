package com.prosper.learn.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.dto.HeatmapDataDTO;
import com.prosper.learn.analytics.dto.HeatmapDayDTO;
import com.prosper.learn.analytics.stats.mapper.UserLearningDailyDO;
import com.prosper.learn.analytics.stats.mapper.UserLearningDailyMapper;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户学习统计服务
 *
 * 提供学习行为记录和热力图数据查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLearningStatsService {

    private final UserLearningDailyMapper userLearningDailyMapper;
    private final UserStatsYearlyMapper userStatsYearlyMapper;
    private final ObjectMapper objectMapper;

    // ===== 写入接口 =====

    /**
     * 记录完成节点
     *
     * @param userId 用户ID
     * @param count 完成数量（默认1）
     */
    public void recordCompletedNodes(long userId, int count) {
        if (count <= 0) return;

        LocalDate today = TimeZoneUtil.now();
        userLearningDailyMapper.incrementCompletedNodes(userId, today, count);
        log.debug("记录用户{}完成{}个节点", userId, count);
    }

    /**
     * 记录完成节点（默认1个）
     */
    public void recordCompletedNode(long userId) {
        recordCompletedNodes(userId, 1);
    }

    /**
     * 记录取消完成节点
     *
     * @param userId 用户ID
     */
    public void recordUncompletedNode(long userId) {
        LocalDate today = TimeZoneUtil.now();
        userLearningDailyMapper.decrementCompletedNodes(userId, today, 1);
        log.debug("记录用户{}取消完成1个节点", userId);
    }

    /**
     * 记录复习卡片
     *
     * @param userId 用户ID
     * @param count 复习数量
     */
    public void recordReviewedCards(long userId, int count) {
        if (count <= 0) return;

        LocalDate today = TimeZoneUtil.now();
        userLearningDailyMapper.incrementReviewedCards(userId, today, count);
        log.debug("记录用户{}复习{}张卡片", userId, count);
    }

    // ===== 查询接口 =====

    /**
     * 获取热力图数据
     *
     * @param userId 用户ID
     * @param months 月数（默认12个月）
     * @return 热力图数据
     */
    public HeatmapDataDTO getHeatmapData(long userId, int months) {
        LocalDate endDate = TimeZoneUtil.now();
        LocalDate startDate = endDate.minusMonths(months);

        // 收集所有日期的数据
        Map<String, HeatmapDayDTO> dataMap = new HashMap<>();

        // 1. 从 yearly 表获取历史数据
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            Map<String, int[]> yearData = getYearLearningStats(userId, year);
            for (Map.Entry<String, int[]> entry : yearData.entrySet()) {
                String dayKey = entry.getKey();
                int[] stats = entry.getValue();

                LocalDate date = parseDayKey(year, dayKey);
                if (date != null && !date.isBefore(startDate) && !date.isAfter(endDate)) {
                    String dateStr = date.toString();
                    int completedNodes = stats.length > 4 ? stats[4] : 0;
                    int reviewedCards = stats.length > 5 ? stats[5] : 0;
                    int activityValue = completedNodes * 10 + reviewedCards;

                    dataMap.put(dateStr, HeatmapDayDTO.builder()
                        .date(dateStr)
                        .completedNodes(completedNodes)
                        .reviewedCards(reviewedCards)
                        .activityValue(activityValue)
                        .build());
                }
            }
        }

        // 2. 从 daily 表获取今日数据（覆盖 yearly 中可能存在的旧数据）
        LocalDate today = TimeZoneUtil.now();
        UserLearningDailyDO todayData = userLearningDailyMapper.getByUserIdAndDate(userId, today);
        if (todayData != null) {
            int completedNodes = todayData.getCompletedNodes() != null ? todayData.getCompletedNodes() : 0;
            int reviewedCards = todayData.getReviewedCards() != null ? todayData.getReviewedCards() : 0;
            int activityValue = completedNodes * 10 + reviewedCards;

            dataMap.put(today.toString(), HeatmapDayDTO.builder()
                .date(today.toString())
                .completedNodes(completedNodes)
                .reviewedCards(reviewedCards)
                .activityValue(activityValue)
                .build());
        }

        // 3. 计算汇总数据
        int totalCompletedNodes = 0;
        int totalReviewedCards = 0;
        int activeDays = 0;

        for (HeatmapDayDTO day : dataMap.values()) {
            totalCompletedNodes += day.getCompletedNodes();
            totalReviewedCards += day.getReviewedCards();
            if (day.getActivityValue() > 0) {
                activeDays++;
            }
        }

        // 4. 转换为列表并排序
        List<HeatmapDayDTO> dailyData = new ArrayList<>(dataMap.values());
        dailyData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        return HeatmapDataDTO.builder()
            .userId(userId)
            .startDate(startDate.toString())
            .endDate(endDate.toString())
            .totalCompletedNodes(totalCompletedNodes)
            .totalReviewedCards(totalReviewedCards)
            .activeDays(activeDays)
            .dailyData(dailyData)
            .build();
    }

    /**
     * 获取用户指定年份的学习统计数据
     *
     * @return Map<dayKey, int[]> 其中 int[] = [views, twice, like, comments, completedNodes, reviewedCards]
     */
    private Map<String, int[]> getYearLearningStats(long userId, int year) {
        Map<String, int[]> result = new HashMap<>();

        try {
            UserStatsYearlyDO yearStats = userStatsYearlyMapper.getByUserIdAndYear(userId, year);
            if (yearStats == null || yearStats.getStats() == null) {
                return result;
            }

            // 解析 JSON: {"1-15": [v, t, l, c, cn, rc], ...}
            Map<String, List<Integer>> statsMap = objectMapper.readValue(
                yearStats.getStats(),
                new TypeReference<Map<String, List<Integer>>>() {}
            );

            for (Map.Entry<String, List<Integer>> entry : statsMap.entrySet()) {
                List<Integer> values = entry.getValue();
                int[] arr = new int[6];
                for (int i = 0; i < Math.min(values.size(), 6); i++) {
                    arr[i] = values.get(i) != null ? values.get(i) : 0;
                }
                result.put(entry.getKey(), arr);
            }

        } catch (Exception e) {
            log.error("解析用户{}的{}年学习统计数据失败", userId, year, e);
        }

        return result;
    }

    /**
     * 解析 dayKey 为 LocalDate
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
            log.warn("解析dayKey失败: year={}, dayKey={}", year, dayKey);
        }
        return null;
    }
}
