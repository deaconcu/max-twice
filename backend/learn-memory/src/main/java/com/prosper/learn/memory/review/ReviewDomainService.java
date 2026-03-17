package com.prosper.learn.memory.review;

import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 复习领域服务
 *
 * 只依赖 memory 域，处理 Anki 复习算法的核心业务逻辑
 *
 * 核心改进：LEARNING/RELEARNING 阶段使用卡片计数（reappearAt）而非时间间隔
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewDomainService {

    private final UserCardSrsDataService srsDataService;
    private final UserCourseSrsSettingDataService courseSrsSettingDataService;
    private final DailyLimitService dailyLimitService;
    private final SystemProperties systemProperties;

    // ========== Query 方法 ==========

    /**
     * 获取下一张待复习卡片（需要指定课程）
     *
     * @param userId 用户ID
     * @param courseId 课程ID（必须指定）
     * @param reviewCardCount 用户当前的复习卡片计数
     * @param newFirst true=先新卡后复习，false=先复习后新卡
     * @return SRS 状态，无卡片时返回 null
     */
    public UserCardSrsDO getNextCard(Long userId, Long courseId, long reviewCardCount, boolean newFirst) {
        if (courseId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("必须指定课程");
        }

        // 获取课程设置
        UserCourseSrsSettingDO setting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (setting == null) {
            throw StatusCode.MEMORY_BANK_COURSE_NOT_FOUND.exception();
        }

        // 获取每日上限
        int dailyNewLimit = setting.getDailyNewLimit() != null
            ? setting.getDailyNewLimit()
            : systemProperties.getSrs().getDefaultDailyNewLimit();
        int dailyReviewLimit = setting.getDailyReviewLimit() != null
            ? setting.getDailyReviewLimit()
            : systemProperties.getSrs().getDefaultDailyReviewLimit();

        // 从 Redis 获取今日计数
        int todayNewCount = dailyLimitService.getTodayNewCount(userId, courseId);
        int todayReviewCount = dailyLimitService.getTodayReviewCount(userId, courseId);

        boolean canShowNew = todayNewCount < dailyNewLimit;
        boolean canShowReview = todayReviewCount < dailyReviewLimit;

        // 1. 学习中到期的优先（不受上限限制，因为已经开始学了）
        UserCardSrsDO card = srsDataService.getNextLearningCardByCourse(userId, courseId, reviewCardCount);
        if (card != null) {
            return card;
        }

        // 2. 根据 newFirst 和上限决定顺序
        if (newFirst) {
            if (canShowNew) {
                card = srsDataService.getNextNewCardByCourse(userId, courseId);
                if (card != null) {
                    return card;
                }
            }
            if (canShowReview) {
                card = srsDataService.getNextReviewCardByCourse(userId, courseId);
                if (card != null) {
                    return card;
                }
            }
        } else {
            if (canShowReview) {
                card = srsDataService.getNextReviewCardByCourse(userId, courseId);
                if (card != null) {
                    return card;
                }
            }
            if (canShowNew) {
                card = srsDataService.getNextNewCardByCourse(userId, courseId);
                if (card != null) {
                    return card;
                }
            }
        }

        // 3. 兜底：学习中未到期的（不受上限限制）
        return srsDataService.getNextPendingLearningCardByCourse(userId, courseId, reviewCardCount);
    }

    // ========== Command 方法 ==========

    /**
     * 提交复习结果 - 基于卡片计数的算法实现
     *
     * @param userId 用户ID
     * @param cardId 卡片ID
     * @param courseId 课程ID
     * @param rating 评级（1-4）
     * @param reviewCardCount 用户当前的复习卡片计数（已递增后的值）
     * @return 更新后的 SRS 状态
     */
    @Transactional
    public UserCardSrsDO submitReview(Long userId, Long cardId, Long courseId, int rating, long reviewCardCount) {
        // 获取SRS状态
        UserCardSrsDO card = srsDataService.getByUserAndCard(userId, cardId);
        if (card == null) {
            throw StatusCode.SRS_STATE_NOT_FOUND.exception();
        }

        // 记录原始类型，用于判断是否需要更新计数
        byte originalType = card.getType();

        int[] cardGaps = systemProperties.getSrs().getAlgorithm().getCardGaps();

        // 根据卡片类型分发到不同的处理方法
        switch (card.getType()) {
            case UserCardSrsDO.TYPE_NEW:
                handleNewCard(card, rating, reviewCardCount, cardGaps);
                break;
            case UserCardSrsDO.TYPE_LEARNING:
            case UserCardSrsDO.TYPE_RELEARNING:
                handleLearningCard(card, rating, reviewCardCount, cardGaps);
                break;
            case UserCardSrsDO.TYPE_REVIEW:
                handleReviewCard(card, rating, reviewCardCount, cardGaps);
                break;
            default:
                throw StatusCode.INVALID_PARAMETER.exception("未知的卡片类型: " + card.getType());
        }

        // 更新时间戳
        card.setLastReviewedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        srsDataService.update(card);

        // 更新今日计数（Redis）
        if (courseId != null) {
            if (originalType == UserCardSrsDO.TYPE_NEW) {
                // 新卡片首次学习
                dailyLimitService.incrementNewCount(userId, courseId);
            } else if (originalType == UserCardSrsDO.TYPE_REVIEW) {
                // 复习卡片
                dailyLimitService.incrementReviewCount(userId, courseId);
            }
            // LEARNING/RELEARNING 不计入上限，因为它们是同一次学习的延续
        }

        log.info("Submitted review for user: {} card: {} type: {} rating: {} reappearAt: {} reviewDueAt: {}",
            userId, cardId, card.getType(), rating, card.getReappearAt(), card.getReviewDueAt());

        return card;
    }

    // ========== Anki 算法核心实现（基于卡片计数） ==========

    /**
     * 处理新卡片 (NEW -> LEARNING 或 REVIEW)
     */
    private void handleNewCard(UserCardSrsDO card, int rating, long reviewCardCount, int[] cardGaps) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();

        if (rating == 1 || rating == 2) {
            // 评级"重来"(1) 或 "困难"(2): 进入学习流程 Step 0
            card.setType(UserCardSrsDO.TYPE_LEARNING);
            card.setCurrentStep((byte) 0);
            card.setReappearAt(reviewCardCount + cardGaps[0]);
            card.setReviewDueAt(null);

        } else if (rating == 3) {
            // 评级"良好"(3): 跳过第一步，进入 Step 1 或直接毕业
            if (cardGaps.length <= 1) {
                graduateToReview(card, config.getGraduatingInterval());
            } else {
                card.setType(UserCardSrsDO.TYPE_LEARNING);
                card.setCurrentStep((byte) 1);
                card.setReappearAt(reviewCardCount + cardGaps[1]);
                card.setReviewDueAt(null);
            }

        } else if (rating == 4) {
            // 评级"简单"(4): 立即毕业
            graduateToReview(card, config.getEasyInterval());
        }
    }

    /**
     * 处理学习中/重新学习中的卡片 (LEARNING/RELEARNING)
     */
    private void handleLearningCard(UserCardSrsDO card, int rating, long reviewCardCount, int[] cardGaps) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();
        byte currentStep = card.getCurrentStep();
        boolean isRelearning = (card.getType() == UserCardSrsDO.TYPE_RELEARNING);

        if (rating == 1) {
            // 评级"重来"(1): 重置到第一步
            card.setCurrentStep((byte) 0);
            card.setReappearAt(reviewCardCount + cardGaps[0]);

        } else if (rating == 2) {
            // 评级"困难"(2): 保持当前步骤，间隔取平均值
            int gap;
            if (currentStep >= cardGaps.length - 1) {
                gap = cardGaps[currentStep];
            } else {
                gap = (cardGaps[currentStep] + cardGaps[currentStep + 1]) / 2;
            }
            card.setReappearAt(reviewCardCount + gap);

        } else if (rating == 3) {
            // 评级"良好"(3): 推进到下一步或毕业
            byte nextStep = (byte) (currentStep + 1);
            if (nextStep >= cardGaps.length) {
                if (isRelearning) {
                    regraduateToReview(card, config, false);
                } else {
                    graduateToReview(card, config.getGraduatingInterval());
                }
            } else {
                card.setCurrentStep(nextStep);
                card.setReappearAt(reviewCardCount + cardGaps[nextStep]);
            }

        } else if (rating == 4) {
            // 评级"简单"(4): 立即毕业
            if (isRelearning) {
                regraduateToReview(card, config, true);
            } else {
                graduateToReview(card, config.getEasyInterval());
            }
        }
    }

    /**
     * 处理复习卡片 (REVIEW)
     */
    private void handleReviewCard(UserCardSrsDO card, int rating, long reviewCardCount, int[] cardGaps) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();
        int currentInterval = card.getInterval() != null ? card.getInterval() : 1;
        BigDecimal ef = card.getEaseFactor();

        if (rating == 1) {
            // 评级"重来"(1): 遗忘，进入重新学习
            card.setLapseOldInterval((short) currentInterval);
            card.setType(UserCardSrsDO.TYPE_RELEARNING);
            card.setCurrentStep((byte) 0);
            card.setRepetitions(0);
            card.setLapseCount(card.getLapseCount() + 1);
            card.setEaseFactor(updateEaseFactor(ef, -0.20, config.getMinEaseFactor()));
            card.setReappearAt(reviewCardCount + cardGaps[0]);
            card.setReviewDueAt(null);

        } else if (rating == 2) {
            // 评级"困难"(2): 间隔增长放缓
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * 1.2);
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            card.setReappearAt(null);
            card.setEaseFactor(updateEaseFactor(ef, -0.15, config.getMinEaseFactor()));

        } else if (rating == 3) {
            // 评级"良好"(3): 标准间隔增长
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * ef.doubleValue());
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            card.setReappearAt(null);

        } else if (rating == 4) {
            // 评级"简单"(4): 额外奖励
            card.setRepetitions(card.getRepetitions() + 1);
            int newInterval = (int) (currentInterval * ef.doubleValue() * config.getEasyBonus());
            card.setInterval(newInterval);
            card.setReviewDueAt(LocalDateTime.now().plusDays(newInterval));
            card.setReappearAt(null);
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
        card.setReappearAt(null);
        card.setReviewDueAt(LocalDateTime.now().plusDays(intervalDays));
    }

    /**
     * 重新毕业到复习状态 (RELEARNING -> REVIEW)
     */
    private void regraduateToReview(UserCardSrsDO card, SystemProperties.Srs.Algorithm config, boolean useEasyInterval) {
        card.setType(UserCardSrsDO.TYPE_REVIEW);
        card.setCurrentStep((byte) 0);

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
        card.setLapseOldInterval(null);
        card.setReappearAt(null);
        card.setReviewDueAt(LocalDateTime.now().plusDays(recoveredInterval));
    }

    /**
     * 更新难度系数 (EF)
     */
    private BigDecimal updateEaseFactor(BigDecimal currentEF, double delta, double minEF) {
        double newEF = currentEF.doubleValue() + delta;
        return BigDecimal.valueOf(Math.max(newEF, minEF));
    }
}
