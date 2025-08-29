package com.prosper.learn.domain.service.basic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.persistence.dataobject.PostStatsDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.mapper.PostStatsMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 评分计算服务
 * 负责根据点赞数据和时间加权算法计算文章和路线图的分数
 * 支持Post和Roadmap两种类型的评分计算
 *
 * 1. 加权得分 S
 * 公式：S = 0.6×C1 + 0.3×C2
 * 含义：根据用户反馈的类型，给每种反馈赋予不同的权重：
 * C1（两次能懂）：权重较高，表示内容需要用户思考才能理解。
 * C2（有帮助）：权重较低，表示内容有一定价值但不够易懂。
 * 通过加权计算出一个总分 S，反映内容的"理解难度"。
 *
 * 2. 时间加权
 * 点赞越久远，权重越低。保证当前的点赞数据更能反映内容的实际易懂度。
 * 可调参数：
 * TIME_DECAY_HALF_LIFE = 30.0：半衰期设为30天，意味着30天前的数据权重为0.5
 * MIN_TIME_WEIGHT = 0.1：最小权重为0.1，防止过老数据权重完全为0
 * TIME_WEIGHT_TYPE = 1：权重类型，支持3种衰减策略
 *
 * 提供了3种时间衰减策略：
 * 类型1（指数衰减）：使用半衰期公式 weight = 0.5^(days/half_life)
 * 类型2（线性衰减）：线性下降到最小权重
 * 类型3（对数衰减）：较缓慢的衰减，适合长期内容
 *
 * 权重效果对比：
 * 使用新的算法（半衰期30天）：
 * 今天：权重 = 1.0
 * 30天前：权重 = 0.5
 * 60天前：权重 = 0.25
 * 90天前：权重 = 0.125（但不会低于0.1）
 *
 * 3. 贝叶斯平滑 p
 * 公式：p = (S_weighted + n0×m0) / (T_weighted + n0)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculationService {

    /** 帖子统计数据访问接口 */
    private final PostStatsMapper postStatsMapper;

    /** 帖子数据访问接口 */
    private final PostMapper postMapper;

    /** 路线图数据访问接口 */
    private final RoadmapMapper roadmapMapper;

    /** 评论数据访问接口 */
    private final CommentMapper commentMapper;

    /** JSON对象映射器，用于统计数据的序列化和反序列化 */
    private final ObjectMapper objectMapper;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    // 算法参数常量
    private static final double ALPHA = 1.5;  // 贝叶斯平滑参数
    private static final double BETA = 5.0;   // 贝叶斯平滑参数

    private static final int MAX_DAYS_HISTORY = 720;  // 最多考虑历史数据天数

    private static final double TWICE_WEIGHT = 0.6;  // 两次能懂的权重
    private static final double HELPFUL_WEIGHT = 0.3;  // 有帮助的权重

    // 时间权重参数
    private static final double TIME_DECAY_HALF_LIFE = 30.0;  // 半衰期（天）
    private static final double MIN_TIME_WEIGHT = 0.1;        // 最小时间权重
    private static final int TIME_WEIGHT_TYPE = 2;            // 权重类型

    // 贝叶斯平滑先验参数
    private static final double PRIOR_MEAN = 0.6;     // 先验均值
    private static final double PRIOR_STRENGTH = 8.0; // 先验强度

    // 分数更新间隔配置（分钟）
    private static final int SCORE_UPDATE_INTERVAL_MINUTES = 1;
    
    // 评论评分权重常量
    private static final double COMMENT_UPVOTE_WEIGHT = 2.0;  // 评论点赞权重
    private static final double COMMENT_REPLY_WEIGHT = 1.0;   // 评论回复权重
    private static final double COMMENT_TIME_BASE_WEIGHT = 1.0; // 评论时间基础权重
    
    // 基准时间常量
    private static final LocalDate BASELINE_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDateTime COMMENT_BASE_TIME = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    /**
     * 验证帖子对象有效性
     * 
     * @param post 帖子对象
     * @throws BusinessException 当帖子对象无效时抛出异常
     */
    private void validatePost(PostDO post) {
        if (post == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子对象不能为空");
        }
        if (post.getId() <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子ID无效: " + post.getId());
        }
    }
    
    /**
     * 验证路线图对象有效性
     * 
     * @param roadmap 路线图对象
     * @throws BusinessException 当路线图对象无效时抛出异常
     */
    private void validateRoadmap(RoadmapDO roadmap) {
        if (roadmap == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图对象不能为空");
        }
        if (roadmap.getId() <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("路线图ID无效: " + roadmap.getId());
        }
    }
    
    /**
     * 验证评论对象有效性
     * 
     * @param comment 评论对象
     * @throws BusinessException 当评论对象无效时抛出异常
     */
    private void validateComment(CommentDO comment) {
        if (comment == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("评论对象不能为空");
        }
        if (comment.getId() <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("评论ID无效: " + comment.getId());
        }
    }
    
    /**
     * 安全地执行JSON处理
     * 
     * @param jsonStr JSON字符串
     * @return 解析后的Map对象
     * @throws BusinessException 当JSON处理失败时抛出异常
     */
    private Map<String, Map<String, Integer>> parseYearlyStatsJson(String jsonStr) {
        try {
            return objectMapper.readValue(jsonStr, new TypeReference<>() {});
        } catch (Exception e) {
            throw ErrorCode.JSON_PROCESSING_ERROR.exception( e);
        }
    }

    /**
     * 计算单个文章的分数
     * 
     * @param post 文章对象
     * @return 文章分数
     * @throws BusinessException 当参数无效或计算失败时抛出异常
     */
    public double calculatePostScore(PostDO post) {
        validatePost(post);
        
        try {
            // 1. 获取历史点赞数据并计算时间加权
            Map<LocalDate, DailyData> dailyData = getDailyUpvoteHistory(post.getId(), Enums.PostStatsType.POST);
            TimeWeightedData timeWeightedData = calculateTimeWeightedData(dailyData);

            // 2. 使用时间加权后的数据进行贝叶斯平滑
            double bayesianRate = calculateBayesianSmoothedRate(
                timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount);

            // 3. 计算LCB (Lower Confidence Bound)
            double lcb = calculateLCB(bayesianRate, timeWeightedData.weightedSampleCount);

            log.debug("文章ID: {}, 时间加权分数: {}, 时间加权样本数: {}, 贝叶斯率: {}, LCB: {}",
                     post.getId(), timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount, 
                     bayesianRate, lcb);

            return lcb;

        } catch (Exception e) {
            log.error("计算文章分数失败, postId: {}", post.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 计算单个路线图的分数
     * 
     * @param roadmap 路线图对象
     * @return 路线图分数
     * @throws BusinessException 当参数无效或计算失败时抛出异常
     */
    public double calculateRoadmapScore(RoadmapDO roadmap) {
        validateRoadmap(roadmap);
        
        try {
            // 1. 获取历史点赞数据并计算时间加权
            Map<LocalDate, DailyData> dailyData = getDailyUpvoteHistory(roadmap.getId(), Enums.PostStatsType.ROADMAP);
            TimeWeightedData timeWeightedData = calculateTimeWeightedData(dailyData);

            // 2. 使用时间加权后的数据进行贝叶斯平滑
            double bayesianRate = calculateBayesianSmoothedRate(
                timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount);

            // 3. 计算LCB (Lower Confidence Bound)
            double lcb = calculateLCB(bayesianRate, timeWeightedData.weightedSampleCount);

            log.debug("路线图ID: {}, 时间加权分数: {}, 时间加权样本数: {}, 贝叶斯率: {}, LCB: {}",
                     roadmap.getId(), timeWeightedData.weightedScore, timeWeightedData.weightedSampleCount,
                     bayesianRate, lcb);

            return lcb;

        } catch (Exception e) {
            log.error("计算路线图分数失败, roadmapId: {}", roadmap.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 时间加权数据结构
     */
    private static class TimeWeightedData {
        final double weightedScore;      // 时间加权后的总分数
        final double weightedSampleCount; // 时间加权后的样本数
        
        TimeWeightedData(double weightedScore, double weightedSampleCount) {
            this.weightedScore = weightedScore;
            this.weightedSampleCount = weightedSampleCount;
        }
    }

    /**
     * 每日数据结构，包含分数和样本数
     */
    private static class DailyData {
        final double score;      // 当天的加权分数
        final int sampleCount;   // 当天的样本数（总点赞次数）

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

            // 计算时间权重：越新的数据权重越高
            //long daysFromToday = ChronoUnit.DAYS.between(date, today);
            //double timeWeight = calculateTimeWeight(daysFromToday);
            double timeWeight = calculateTimeWeightFromBaseline(date);

            // 累加时间加权后的分数和样本数
            weightedScoreSum += data.score * timeWeight;
            weightedSampleSum += data.sampleCount * timeWeight;
        }

        return new TimeWeightedData(weightedScoreSum, weightedSampleSum);
    }

    /**
     * 获取文章或路线图的每日点赞历史数据，包含分数和样本数
     */
    private Map<LocalDate, DailyData> getDailyUpvoteHistory(long objectId, Enums.PostStatsType objectType) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(MAX_DAYS_HISTORY);

        Map<LocalDate, DailyData> dailyData = new HashMap<>();

        // 获取涉及的年份范围
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            PostStatsDO yearStats = postStatsMapper.getByTypeAndObjectIdAndYear( objectType.value(), (long) objectId, year);

            if (yearStats != null) {
                try {
                    Map<String, Map<String, Integer>> yearlyData = objectMapper.readValue(
                        yearStats.getStats(), new TypeReference<>() {});

                    // 遍历指定日期范围
                    LocalDate currentDate = (year == startYear) ? startDate : LocalDate.of(year, 1, 1);
                    LocalDate lastDate = (year == endYear) ? endDate : LocalDate.of(year, 12, 31);

                    while (!currentDate.isAfter(lastDate)) {
                        String dayKey = currentDate.getMonthValue() + "-" + currentDate.getDayOfMonth();
                        Map<String, Integer> dayStats = yearlyData.get(dayKey);

                        if (dayStats != null) {
                            int twice = dayStats.getOrDefault("twice", 0);
                            int helpful = dayStats.getOrDefault("helpful", 0);

                            // 计算当天的加权分数 (移除once，只保留twice和helpful)
                            double dayScore = twice * TWICE_WEIGHT + helpful * HELPFUL_WEIGHT;
                            // 计算当天的样本数（总点赞次数）
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
     * 计算时间权重
     * 支持多种衰减策略，可调参数
     */
    private double calculateTimeWeight(long days) {
        if (days < 0) return 1.0; // 未来的数据（理论上不存在）给予最高权重

        double weight;
        switch ((int)TIME_WEIGHT_TYPE) {
            case 1: // 指数衰减（基于半衰期）
                // 使用半衰期公式：weight = 0.5^(days/half_life)
                weight = Math.pow(0.5, days / TIME_DECAY_HALF_LIFE);
                break;
            case 2: // 线性衰减
                // 线性下降到最小权重
                weight = Math.max(MIN_TIME_WEIGHT, 1.0 - (double)days / (TIME_DECAY_HALF_LIFE * 2));
                break;
            case 3: // 对数衰减（较缓慢的衰减）
                // 对数衰减，权重下降更平缓
                weight = Math.max(MIN_TIME_WEIGHT, 1.0 / (1.0 + Math.log1p(days / TIME_DECAY_HALF_LIFE)));
                break;
            default:
                weight = Math.max(MIN_TIME_WEIGHT, Math.pow(0.5, days / TIME_DECAY_HALF_LIFE));
        }

        // 确保权重不低于最小值
        return Math.max(MIN_TIME_WEIGHT, weight);
    }

    /**
     * 计算基于基准日期的时间权重增长
     * 从 2025-1-1 开始，权重为 1，随时间增长
     *
     * @param targetDate 目标日期
     * @return 时间权重（最小为1.0）
     */
    private double calculateTimeWeightFromBaseline(LocalDate targetDate) {
        LocalDate baselineDate = LocalDate.of(2025, 1, 1);
        long daysSinceBaseline = ChronoUnit.DAYS.between(baselineDate, targetDate);

        if (daysSinceBaseline <= 0) {
            return 1.0; // 基准日期或之前，权重为1
        }

        double weight;
        switch ((int)TIME_WEIGHT_TYPE) {
            case 1: // 指数增长
                // 使用指数增长公式：weight = 1 + (e^(days/growth_factor) - 1)
                // growth_factor 控制增长速度，值越大增长越慢
                double growthFactor = 600.0; // 60天的增长因子
                weight = 1.0 + (Math.exp(daysSinceBaseline / growthFactor) - 1.0);
                break;

            case 2: // 线性增长
                // 线性增长：每30天权重增加0.5
                double linearGrowthRate = 0.2 / 30.0; // 每天增长率
                weight = 1.0 + daysSinceBaseline * linearGrowthRate;
                break;

            case 3: // 对数增长
                // 对数增长：weight = 1 + log(1 + days/log_factor)
                double logFactor = 30.0; // 对数增长因子
                weight = 1.0 + Math.log1p(daysSinceBaseline / logFactor);
                break;

            default:
                // 默认使用线性增长
                weight = 1.0 + daysSinceBaseline * (0.5 / 30.0);
        }

        return Math.max(1.0, weight); // 确保权重不低于1.0
    }

    /**
     * 使用时间加权数据计算贝叶斯平滑的点赞率
     */
    private double calculateBayesianSmoothedRate(double score, double sampleCount) {
        if (sampleCount == 0) {
            return PRIOR_MEAN;
        }

        // 贝叶斯平滑公式：p = (S_weighted + n0×m0) / (T_weighted + n0)
        return (score + PRIOR_STRENGTH * PRIOR_MEAN) / (sampleCount + PRIOR_STRENGTH);
    }

    /**
     * 计算Lower Confidence Bound
     */
    private double calculateLCB(double rate, double sampleCount) {
        if (sampleCount == 0) return 0.0;

        // 使用Wilson score interval的下界
        double z = 1.96; // 95%置信区间

        double denominator = 1 + z * z / sampleCount;
        double numerator = rate + z * z / (2 * sampleCount) - z * Math.sqrt((rate * (1 - rate) + z * z / (4 * sampleCount)) / sampleCount);

        return numerator / denominator;
    }

    /**
     * 检查并更新文章分数（如果需要的话）
     * 
     * @param post 文章对象
     * @return 是否进行了分数更新
     * @throws BusinessException 当参数无效时抛出异常
     */
    public boolean checkAndUpdatePostScore(PostDO post) {
        validatePost(post);
        
        try {
            // 检查是否需要更新分数
            if (shouldUpdateScore(post)) {
                double score = calculatePostScore(post);
                postMapper.updateScore(post.getId(), score);
                log.debug("实时更新文章分数: postId={}, score={}", post.getId(), score);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新文章分数失败: postId={}", post.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 检查并更新路线图分数（如果需要的话）
     * 
     * @param roadmap 路线图对象
     * @return 是否进行了分数更新
     * @throws BusinessException 当参数无效时抛出异常
     */
    public boolean checkAndUpdateRoadmapScore(RoadmapDO roadmap) {
        validateRoadmap(roadmap);
        
        try {
            // 检查是否需要更新分数
            if (shouldUpdateScore(roadmap)) {
                double score = calculateRoadmapScore(roadmap);
                roadmapMapper.updateScore(roadmap.getId(), score);
                log.debug("实时更新路线图分数: roadmapId={}, score={}", roadmap.getId(), score);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新路线图分数失败: roadmapId={}", roadmap.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 计算评论的综合评分
     * 
     * 综合排序规则：
     * 1. 点赞数（降序）
     * 2. 回复数（降序）
     * 3. 时间正向增加因子（时间越新，权重越高）
     *
     * @param comment 评论对象
     * @return 综合评分
     * @throws BusinessException 当参数无效或计算失败时抛出异常
     */
    public double calculateCommentScore(CommentDO comment) {
        validateComment(comment);
        
        try {
            // 基础分数：点赞数 + 回复数权重
            double upvoteScore = comment.getUpvoteCount() * COMMENT_UPVOTE_WEIGHT;
            double replyScore = comment.getReplyCount() * COMMENT_REPLY_WEIGHT;

            // 时间正向增加因子：越新的评论权重越高
            double timeWeight = calculateCommentTimeWeight(comment.getCreatedAt());

            // 综合评分 = (点赞分数 + 回复分数) * 时间权重
            double finalScore = (upvoteScore + replyScore) * timeWeight;

            log.debug("评论ID: {}, 点赞数: {}, 回复数: {}, 时间权重: {}, 最终评分: {}",
                     comment.getId(), comment.getUpvoteCount(), comment.getReplyCount(),
                     timeWeight, finalScore);

            return Math.max(0.0, finalScore);

        } catch (Exception e) {
            log.error("计算评论分数失败, commentId: {}", comment.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 计算评论的时间权重（正向增加因子）
     * 基于固定基准时间2025-1-1 00:00:00，时间越新，权重越高，无上限
     *
     * @param createTime 评论创建时间
     * @return 时间权重
     */
    private double calculateCommentTimeWeight(LocalDateTime createTime) {
        if (createTime == null) {
            return COMMENT_TIME_BASE_WEIGHT;  // 使用常量默认权重
        }

        // 使用常量基准时间
        long hoursFromBase = ChronoUnit.HOURS.between(COMMENT_BASE_TIME, createTime);

        // 使用对数增长函数，避免权重过大
        double timeWeight = COMMENT_TIME_BASE_WEIGHT + Math.log1p(Math.max(0, hoursFromBase * 0.0002));

        return timeWeight;
    }

    /**
     * 检查并更新评论分数（如果需要的话）
     * 
     * @param comment 评论对象
     * @return 是否进行了分数更新
     * @throws BusinessException 当参数无效时抛出异常
     */
    public boolean checkAndUpdateCommentScore(CommentDO comment) {
        validateComment(comment);
        
        try {
            double score = calculateCommentScore(comment);

            // 如果分数有变化，则更新
            if (Math.abs(comment.getScore() - score) > 0.001) {
                comment.setScore(score);
                return true;  // 返回true表示需要更新数据库
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新评论分数失败: commentId={}", comment.getId(), e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 判断是否需要更新分数
     * @param post 文章对象
     * @return 是否需要更新
     */
    private boolean shouldUpdateScore(PostDO post) {
        // 检查距离上次计算的时间间隔
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCalculated = post.getScoreCalculatedAt();
        long minutesSinceLastUpdate = ChronoUnit.MINUTES.between(lastCalculated, now);

        return minutesSinceLastUpdate >= SCORE_UPDATE_INTERVAL_MINUTES;
    }

    /**
     * 判断是否需要更新分数
     * @param roadmap 路线图对象
     * @return 是否需要更新
     */
    private boolean shouldUpdateScore(RoadmapDO roadmap) {
        // 检查距离上次计算的时间间隔
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastCalculated = roadmap.getScoreCalculatedAt();
        long minutesSinceLastUpdate = ChronoUnit.MINUTES.between(lastCalculated, now);

        return minutesSinceLastUpdate >= SCORE_UPDATE_INTERVAL_MINUTES;
    }
}
