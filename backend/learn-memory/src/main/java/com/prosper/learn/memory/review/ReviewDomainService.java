package com.prosper.learn.memory.review;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 复习领域服务
 *
 * 只依赖 memory 域，处理 Anki 复习算法的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewDomainService {

    private final UserCardSrsDataService srsStateDataService;
    private final SystemProperties systemProperties;

    // ========== Query 方法 ==========

    /**
     * 获取复习队列
     *
     * @param userId 用户ID
     * @param dueOnly 是否只返回到期的卡片
     * @param courseId 课程ID（可选，用于筛选）
     * @param limit 数量限制
     * @param lastId 分页游标
     * @return SRS 状态列表
     */
    public List<UserCardSrsDO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit, Long lastId) {
        // 参数默认值
        if (dueOnly == null) dueOnly = true;
        if (limit == null) limit = 50;
        if (limit > 500) limit = 500;

        List<UserCardSrsDO> userCardList;

        if (courseId != null) {
            // 按课程筛选
            userCardList = srsStateDataService.getReviewQueueByCourse(userId, courseId, dueOnly, limit, lastId);
        } else {
            // 获取所有课程的复习队列
            userCardList = srsStateDataService.getReviewQueue(userId, dueOnly, limit, lastId);
        }

        return userCardList;
    }

    /**
     * 获取复习统计
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据（totalReviews, streakDays, averageScore, timeSpent）
     */
    public ReviewStats getReviewStats(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Long totalReviews = srsStateDataService.countReviewsInPeriod(userId, startTime, endTime);
        Integer streakDays = srsStateDataService.calculateStreakDays(userId);
        Double averageScore = srsStateDataService.calculateAverageScore(userId, startTime, endTime);
        Long timeSpent = srsStateDataService.calculateTimeSpent(userId, startTime, endTime);

        return new ReviewStats(totalReviews, streakDays, averageScore, timeSpent);
    }

    // ========== Command 方法 ==========

    /**
     * 提交复习结果 - Anki 算法实现
     *
     * @param userId 用户ID
     * @param cardId 卡片ID
     * @param rating 评级（1-4）
     */
    @Transactional
    public void submitReview(Long userId, Long cardId, int rating) {
        // 获取SRS状态
        UserCardSrsDO card = srsStateDataService.getByUserAndCard(userId, cardId);
        if (card == null) {
            throw ErrorCode.SRS_STATE_NOT_FOUND.exception();
        }

        // 根据卡片类型分发到不同的处理方法
        switch (card.getType()) {
            case UserCardSrsDO.TYPE_NEW:
                handleNewCard(card, rating);
                break;
            case UserCardSrsDO.TYPE_LEARNING:
                handleLearningCard(card, rating, systemProperties.getSrs().getAlgorithm().getLearningSteps());
                break;
            case UserCardSrsDO.TYPE_REVIEW:
                handleReviewCard(card, rating);
                break;
            case UserCardSrsDO.TYPE_RELEARNING:
                handleLearningCard(card, rating, systemProperties.getSrs().getAlgorithm().getRelearningSteps());
                break;
            default:
                throw ErrorCode.INVALID_PARAMETER.exception("未知的卡片类型: " + card.getType());
        }

        // 更新时间戳
        card.setLastReviewedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        srsStateDataService.update(card);

        log.info("Submitted review for user: {} card: {} type: {} rating: {} nextDue: {}",
            userId, cardId, card.getType(), rating, card.getReviewDueAt());
    }

    // ========== Anki 算法核心实现 ==========

    /**
     * 处理新卡片 (NEW -> LEARNING 或 REVIEW)
     */
    private void handleNewCard(UserCardSrsDO card, int rating) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();

        if (rating == 1 || rating == 2) {
            // 评级"重来"(1) 或 "困难"(2): 进入学习流程
            card.setType(UserCardSrsDO.TYPE_LEARNING);
            card.setCurrentStep((byte) 0);
            card.setInterval(config.getLearningSteps()[0]);
            card.setReviewDueAt(LocalDateTime.now().plusMinutes(config.getLearningSteps()[0]));

        } else if (rating == 3) {
            // 评级"良好"(3): 跳过第一步或直接毕业
            int[] steps = config.getLearningSteps();
            if (steps.length <= 1) {
                // 只有一步，直接毕业
                graduateToReview(card, config.getGraduatingInterval());
            } else {
                // 跳到第二步
                card.setType(UserCardSrsDO.TYPE_LEARNING);
                card.setCurrentStep((byte) 1);
                card.setInterval(steps[1]);
                card.setReviewDueAt(LocalDateTime.now().plusMinutes(steps[1]));
            }

        } else if (rating == 4) {
            // 评级"简单"(4): 立即毕业
            graduateToReview(card, config.getEasyInterval());
        }
    }

    /**
     * 处理学习中/重新学习中的卡片 (LEARNING/RELEARNING)
     */
    private void handleLearningCard(UserCardSrsDO card, int rating, int[] steps) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();
        byte currentStep = card.getCurrentStep();
        boolean isRelearning = (card.getType() == UserCardSrsDO.TYPE_RELEARNING);

        if (rating == 1) {
            // 评级"重来"(1): 重置到第一步
            card.setCurrentStep((byte) 0);
            card.setInterval(steps[0]);
            card.setReviewDueAt(LocalDateTime.now().plusMinutes(steps[0]));
            // 保持 lapseOldInterval 不变（如果是 RELEARNING）

        } else if (rating == 2) {
            // 评级"困难"(2): 当前步骤延长
            int extendedInterval;
            if (currentStep >= steps.length - 1) {
                // 最后一步，保持当前间隔
                extendedInterval = steps[currentStep];
            } else {
                // 中间步骤，取当前和下一步的平均值
                extendedInterval = (steps[currentStep] + steps[currentStep + 1]) / 2;
            }
            card.setInterval(extendedInterval);
            card.setReviewDueAt(LocalDateTime.now().plusMinutes(extendedInterval));
            // currentStep 不变

        } else if (rating == 3) {
            // 评级"良好"(3): 推进到下一步或毕业
            byte nextStep = (byte) (currentStep + 1);
            if (nextStep >= steps.length) {
                // 完成所有步骤，毕业
                if (isRelearning) {
                    regraduateToReview(card, config, false);  // 使用 graduatingInterval
                } else {
                    graduateToReview(card, config.getGraduatingInterval());
                }
            } else {
                // 进入下一步
                card.setCurrentStep(nextStep);
                card.setInterval(steps[nextStep]);
                card.setReviewDueAt(LocalDateTime.now().plusMinutes(steps[nextStep]));
            }

        } else if (rating == 4) {
            // 评级"简单"(4): 立即毕业
            if (isRelearning) {
                regraduateToReview(card, config, true);  // 使用 easyInterval
            } else {
                graduateToReview(card, config.getEasyInterval());
            }
        }
    }

    /**
     * 处理复习卡片 (REVIEW)
     */
    private void handleReviewCard(UserCardSrsDO card, int rating) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();
        int currentInterval = card.getInterval();
        BigDecimal ef = card.getEaseFactor();

        if (rating == 1) {
            // 评级"重来"(1): 遗忘，进入重新学习
            card.setLapseOldInterval((short) currentInterval);  // 保存遗忘前的间隔
            card.setType(UserCardSrsDO.TYPE_RELEARNING);
            card.setCurrentStep((byte) 0);
            card.setRepetitions(0);  // 重置连续正确次数
            card.setLapseCount(card.getLapseCount() + 1);
            card.setEaseFactor(updateEaseFactor(ef, -0.20, config.getMinEaseFactor()));

            int[] relearningSteps = config.getRelearningSteps();
            card.setInterval(relearningSteps[0]);
            card.setReviewDueAt(LocalDateTime.now().plusMinutes(relearningSteps[0]));

        } else if (rating == 2) {
            // 评级"困难"(2): 间隔增长放缓
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * 1.2);
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            card.setEaseFactor(updateEaseFactor(ef, -0.15, config.getMinEaseFactor()));

        } else if (rating == 3) {
            // 评级"良好"(3): 标准间隔增长
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * ef.doubleValue());
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            // EF 不变

        } else if (rating == 4) {
            // 评级"简单"(4): 额外奖励
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * ef.doubleValue() * config.getEasyBonus());
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            card.setEaseFactor(updateEaseFactor(ef, 0.15, config.getMinEaseFactor()));
        }
    }

    /**
     * 毕业到复习状态 (LEARNING -> REVIEW)
     */
    private void graduateToReview(UserCardSrsDO card, int intervalDays) {
        card.setType(UserCardSrsDO.TYPE_REVIEW);
        card.setCurrentStep((byte) 0);
        card.setInterval(intervalDays);
        card.setLapseOldInterval(null);
        card.setReviewDueAt(LocalDateTime.now().plusDays(intervalDays));
    }

    /**
     * 重新毕业到复习状态 (RELEARNING -> REVIEW)
     * @param card 卡片对象
     * @param config 算法配置
     * @param useEasyInterval 是否使用 easyInterval（true=评级4简单，false=评级3良好）
     */
    private void regraduateToReview(UserCardSrsDO card, SystemProperties.Srs.Algorithm config, boolean useEasyInterval) {
        card.setType(UserCardSrsDO.TYPE_REVIEW);
        card.setCurrentStep((byte) 0);

        // 计算恢复间隔
        Short lapseOldInterval = card.getLapseOldInterval();
        int baseInterval = useEasyInterval ? config.getEasyInterval() : config.getGraduatingInterval();
        int recoveredInterval;

        if (lapseOldInterval != null && lapseOldInterval > 0) {
            recoveredInterval = Math.max(
                baseInterval,
                (int) Math.floor(lapseOldInterval * config.getNewIntervalMultiplier())
            );
        } else {
            recoveredInterval = baseInterval;
        }

        card.setInterval(recoveredInterval);
        card.setLapseOldInterval(null);  // 清空
        card.setReviewDueAt(LocalDateTime.now().plusDays(recoveredInterval));
    }

    /**
     * 更新难度系数 (EF)
     */
    private BigDecimal updateEaseFactor(BigDecimal currentEF, double delta, double minEF) {
        double newEF = currentEF.doubleValue() + delta;
        return BigDecimal.valueOf(Math.max(newEF, minEF));
    }

    // ========== 工具方法 ==========

    /**
     * 根据周期计算开始时间
     */
    public LocalDateTime calculateStartTime(LocalDateTime endTime, String period) {
        switch (period.toUpperCase()) {
            case "DAY":
                return endTime.minusDays(1);
            case "WEEK":
                return endTime.minusDays(7);
            case "MONTH":
                return endTime.minusDays(30);
            case "YEAR":
                return endTime.minusDays(365);
            default:
                return endTime.minusDays(7);
        }
    }

    /**
     * 复习统计数据（内部使用）
     */
    public static class ReviewStats {
        private final Long totalReviews;
        private final Integer streakDays;
        private final Double averageScore;
        private final Long timeSpent;

        public ReviewStats(Long totalReviews, Integer streakDays, Double averageScore, Long timeSpent) {
            this.totalReviews = totalReviews;
            this.streakDays = streakDays;
            this.averageScore = averageScore;
            this.timeSpent = timeSpent;
        }

        public Long getTotalReviews() {
            return totalReviews;
        }

        public Integer getStreakDays() {
            return streakDays;
        }

        public Double getAverageScore() {
            return averageScore;
        }

        public Long getTimeSpent() {
            return timeSpent;
        }
    }
}
