package com.prosper.learn.analytics.scoring;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyMapper;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 评分计算领域服务
 * 只依赖本领域（analytics）模块，处理核心评分算法
 * 负责根据点赞数据和时间加权算法计算内容的分数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculationDomainService {

    private final ContentStatsYearlyMapper contentStatsYearlyMapper;
    private final ContentStatsDataService contentStatsDataService;
    private final ObjectMapper objectMapper;

    // ========== 算法参数常量 ==========

    private static final int MAX_DAYS_HISTORY = 720;

    private static final double TWICE_WEIGHT = 0.6;
    private static final double HELPFUL_WEIGHT = 0.3;

    // 时间权重参数
    private static final double TIME_DECAY_HALF_LIFE = 30.0;
    private static final double MIN_TIME_WEIGHT = 0.1;
    private static final int TIME_WEIGHT_TYPE = 2;

    // 贝叶斯平滑先验参数
    private static final double PRIOR_MEAN = 0.6;
    private static final double PRIOR_STRENGTH = 8.0;

    // 分数更新间隔配置（分钟）
    private static final int SCORE_UPDATE_INTERVAL_MINUTES = 1;

    // 评论评分权重常量
    private static final double COMMENT_UPVOTE_WEIGHT = 2.0;
    private static final double COMMENT_REPLY_WEIGHT = 1.0;
    private static final double COMMENT_TIME_BASE_WEIGHT = 1.0;

    // 基准时间常量
    private static final LocalDate BASELINE_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDateTime COMMENT_BASE_TIME = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    // ========== 核心计算方法 ==========

    /**
     * 计算内容的分数（通用方法，用于 Post/Roadmap）
     */
    public double calculateContentScore(long objectId, Enums.ContentType contentType) {
        try {
            Map<LocalDate, DailyData> dailyData = getDailyUpvoteHistory(objectId, contentType);
            TimeWeightedData timeWeightedData = calculateTimeWeightedData(dailyData);

            double bayesianRate = calculateBayesianSmoothedRate(
                timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount);

            double lcb = calculateLCB(bayesianRate, timeWeightedData.weightedSampleCount);

            log.debug("内容ID: {}, 类型: {}, 时间加权分数: {}, 时间加权样本数: {}, 贝叶斯率: {}, LCB: {}",
                     objectId, contentType, timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount,
                     bayesianRate, lcb);

            return lcb;

        } catch (Exception e) {
            log.error("计算内容分数失败, objectId: {}, contentType: {}", objectId, contentType, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 计算评论的综合评分
     */
    public double calculateCommentScore(long commentId, LocalDateTime createdAt) {
        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(Enums.ContentType.comment, commentId)
                .orElse(null);

            int upvoteCount = 0;
            int replyCount = 0;

            if (stats != null) {
                upvoteCount = stats.getLikes() != null ? stats.getLikes() : 0;
                replyCount = stats.getComments() != null ? stats.getComments() : 0;
            }

            double upvoteScore = upvoteCount * COMMENT_UPVOTE_WEIGHT;
            double replyScore = replyCount * COMMENT_REPLY_WEIGHT;
            double timeWeight = calculateCommentTimeWeight(createdAt);

            double finalScore = (upvoteScore + replyScore) * timeWeight;

            log.debug("评论ID: {}, 点赞数: {}, 回复数: {}, 时间权重: {}, 最终评分: {}",
                     commentId, upvoteCount, replyCount, timeWeight, finalScore);

            return Math.max(0.0, finalScore);

        } catch (Exception e) {
            log.error("计算评论分数失败, commentId: {}", commentId, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 计算记忆卡片组的分数
     */
    public double calculateMemoryCardDeckScore(long deckId, LocalDateTime createdAt, Integer cardCount) {
        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(Enums.ContentType.memory_card_deck, deckId)
                .orElse(null);

            int upvoteCount = 0;
            if (stats != null && stats.getLikes() != null) {
                upvoteCount = stats.getLikes();
            }

            double upvoteScore = upvoteCount * 1.0;
            double timeWeight = calculateTimeWeightFromBaseline(createdAt.toLocalDate());

            int count = cardCount != null ? cardCount : 0;
            double cardCountFactor = Math.log1p(count * 0.1);

            double finalScore = upvoteScore * timeWeight + cardCountFactor;

            log.debug("卡片组ID: {}, 点赞数: {}, 卡片数: {}, 时间权重: {}, 最终评分: {}",
                     deckId, upvoteCount, count, timeWeight, finalScore);

            return Math.max(0.0, finalScore);

        } catch (Exception e) {
            log.error("计算卡片组分数失败, deckId: {}", deckId, e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 判断是否需要更新分数（基于时间间隔）
     */
    public boolean shouldUpdateScore(LocalDateTime lastCalculatedAt) {
        if (lastCalculatedAt == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        long minutesSinceLastUpdate = ChronoUnit.MINUTES.between(lastCalculatedAt, now);
        return minutesSinceLastUpdate >= SCORE_UPDATE_INTERVAL_MINUTES;
    }

    // ========== Private 算法方法 ==========

    /**
     * 时间加权数据结构
     */
    private static class TimeWeightedData {
        final double weightedScore;
        final double weightedSampleCount;

        TimeWeightedData(double weightedScore, double weightedSampleCount) {
            this.weightedScore = weightedScore;
            this.weightedSampleCount = weightedSampleCount;
        }
    }

    /**
     * 每日数据结构
     */
    private static class DailyData {
        final double score;
        final int sampleCount;

        DailyData(double score, int sampleCount) {
            this.score = score;
            this.sampleCount = sampleCount;
        }
    }

    /**
     * 计算时间加权的数据
     */
    private TimeWeightedData calculateTimeWeightedData(Map<LocalDate, DailyData> dailyData) {
        if (dailyData.isEmpty()) {
            return new TimeWeightedData(0.0, 0.0);
        }

        double weightedScoreSum = 0.0;
        double weightedSampleSum = 0.0;
        LocalDate today = LocalDate.now();

        for (Map.Entry<LocalDate, DailyData> entry : dailyData.entrySet()) {
            LocalDate date = entry.getKey();
            DailyData data = entry.getValue();

            double timeWeight = calculateTimeWeightFromBaseline(date);

            weightedScoreSum += data.score * timeWeight;
            weightedSampleSum += data.sampleCount * timeWeight;
        }

        return new TimeWeightedData(weightedScoreSum, weightedSampleSum);
    }

    /**
     * 获取内容的每日点赞历史数据
     */
    private Map<LocalDate, DailyData> getDailyUpvoteHistory(long objectId, Enums.ContentType contentType) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(MAX_DAYS_HISTORY);

        Map<LocalDate, DailyData> dailyData = new HashMap<>();

        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            ContentStatsYearlyDO yearStats = contentStatsYearlyMapper.getByTypeAndObjectIdAndYear(
                contentType.value(), objectId, year);

            if (yearStats != null) {
                try {
                    Map<String, Map<String, Integer>> yearlyData = objectMapper.readValue(
                        yearStats.getStats(), new TypeReference<>() {});

                    LocalDate currentDate = (year == startYear) ? startDate : LocalDate.of(year, 1, 1);
                    LocalDate lastDate = (year == endYear) ? endDate : LocalDate.of(year, 12, 31);

                    while (!currentDate.isAfter(lastDate)) {
                        String dayKey = currentDate.getMonthValue() + "-" + currentDate.getDayOfMonth();
                        Map<String, Integer> dayStats = yearlyData.get(dayKey);

                        if (dayStats != null) {
                            int twice = dayStats.getOrDefault("twice", 0);
                            int helpful = dayStats.getOrDefault("helpful", 0);

                            double dayScore = twice * TWICE_WEIGHT + helpful * HELPFUL_WEIGHT;
                            int daySampleCount = (twice + helpful);

                            if (daySampleCount > 0) {
                                dailyData.put(currentDate, new DailyData(dayScore, daySampleCount));
                            }
                        }

                        currentDate = currentDate.plusDays(1);
                    }
                } catch (Exception e) {
                    log.warn("解析年度统计数据失败: {}", yearStats.getStats(), e);
                }
            }
        }

        return dailyData;
    }

    /**
     * 计算时间权重（旧的衰减方式，保留以备用）
     */
    private double calculateTimeWeight(long days) {
        if (days < 0) return 1.0;

        double weight;
        switch (TIME_WEIGHT_TYPE) {
            case 1:
                weight = Math.pow(0.5, days / TIME_DECAY_HALF_LIFE);
                break;
            case 2:
                weight = Math.max(MIN_TIME_WEIGHT, 1.0 - (double)days / (TIME_DECAY_HALF_LIFE * 2));
                break;
            case 3:
                weight = Math.max(MIN_TIME_WEIGHT, 1.0 / (1.0 + Math.log1p(days / TIME_DECAY_HALF_LIFE)));
                break;
            default:
                weight = Math.max(MIN_TIME_WEIGHT, Math.pow(0.5, days / TIME_DECAY_HALF_LIFE));
        }

        return Math.max(MIN_TIME_WEIGHT, weight);
    }

    /**
     * 计算基于基准日期的时间权重增长
     */
    private double calculateTimeWeightFromBaseline(LocalDate targetDate) {
        long daysSinceBaseline = ChronoUnit.DAYS.between(BASELINE_DATE, targetDate);

        if (daysSinceBaseline <= 0) {
            return 1.0;
        }

        double weight;
        switch (TIME_WEIGHT_TYPE) {
            case 1:
                double growthFactor = 600.0;
                weight = 1.0 + (Math.exp(daysSinceBaseline / growthFactor) - 1.0);
                break;

            case 2:
                double linearGrowthRate = 0.2 / 30.0;
                weight = 1.0 + daysSinceBaseline * linearGrowthRate;
                break;

            case 3:
                double logFactor = 30.0;
                weight = 1.0 + Math.log1p(daysSinceBaseline / logFactor);
                break;

            default:
                weight = 1.0 + daysSinceBaseline * (0.5 / 30.0);
        }

        return Math.max(1.0, weight);
    }

    /**
     * 使用时间加权数据计算贝叶斯平滑的点赞率
     */
    private double calculateBayesianSmoothedRate(double score, double sampleCount) {
        if (sampleCount == 0) {
            return PRIOR_MEAN;
        }

        return (score + PRIOR_STRENGTH * PRIOR_MEAN) / (sampleCount + PRIOR_STRENGTH);
    }

    /**
     * 计算Lower Confidence Bound
     */
    private double calculateLCB(double rate, double sampleCount) {
        if (sampleCount == 0) return 0.0;

        double z = 1.96;

        double denominator = 1 + z * z / sampleCount;
        double numerator = rate + z * z / (2 * sampleCount) - z * Math.sqrt((rate * (1 - rate) + z * z / (4 * sampleCount)) / sampleCount);

        return numerator / denominator;
    }

    /**
     * 计算评论的时间权重（正向增加因子）
     */
    private double calculateCommentTimeWeight(LocalDateTime createTime) {
        if (createTime == null) {
            return COMMENT_TIME_BASE_WEIGHT;
        }

        long hoursFromBase = ChronoUnit.HOURS.between(COMMENT_BASE_TIME, createTime);
        double timeWeight = COMMENT_TIME_BASE_WEIGHT + Math.log1p(Math.max(0, hoursFromBase * 0.0002));

        return timeWeight;
    }
}
