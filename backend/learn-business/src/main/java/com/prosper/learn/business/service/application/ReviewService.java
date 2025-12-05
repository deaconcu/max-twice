package com.prosper.learn.business.service.application;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.response.card.CardWithSrsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.persistence.dataobject.UserCardSrsDO.*;

/**
 * 复习功能业务服务 - Anki 算法实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final UserCardSrsDataService srsStateDataService;
    private final MemoryCardDataService cardDataService;
    private final UserCardSrsConverter srsStateConverter;
    private final MemoryCardService memoryCardService;
    private final UserDataService userDataService;
    private final SystemProperties systemProperties;


    // ========== toDTO ==========

    /**
     * 转换为复习统计DTO
     */
    public ReviewStatsDTO toReviewStatsDTO(Long totalReviews, Integer streakDays, Double averageScore, Long timeSpent) {
        ReviewStatsDTO dto = new ReviewStatsDTO();
        dto.setTotalReviews(totalReviews != null ? totalReviews.intValue() : 0);
        dto.setStreakDays(streakDays != null ? streakDays : 0);
        dto.setAverageScore(averageScore != null ? averageScore : 0.0);
        dto.setTimeSpent(timeSpent != null ? timeSpent.intValue() : 0);
        return dto;
    }

    // ========== 业务方法 ==========

    /**
     * 获取复习队列
     */
    public List<CardWithSrsDTO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit, Long lastId) {
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        
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

        if (userCardList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取卡片信息
        Set<Long> cardIds = userCardList.stream()
            .map(UserCardSrsDO::getCardId)
            .collect(Collectors.toSet());

        List<MemoryCardDO> cardList = cardDataService.getByIds(cardIds);
        return memoryCardService.toCardViewWithSrs(cardList, userId);
    }

    /**
     * 提交复习结果 - Anki 算法实现
     */
    @Transactional
    public void submitReview(Long userId, ReviewCardRequest request) {
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (request == null || request.getCardId() == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("请求参数不能为空");
        }

        // 获取SRS状态
        UserCardSrsDO card = srsStateDataService.getByUserAndCard(userId, request.getCardId());
        if (card == null) {
            throw ErrorCode.SRS_STATE_NOT_FOUND.exception();
        }

        int rating = request.getResult();

        // 根据卡片类型分发到不同的处理方法
        switch (card.getType()) {
            case TYPE_NEW:
                handleNewCard(card, rating);
                break;
            case TYPE_LEARNING:
                handleLearningCard(card, rating, systemProperties.getSrs().getAlgorithm().getLearningSteps());
                break;
            case TYPE_REVIEW:
                handleReviewCard(card, rating);
                break;
            case TYPE_RELEARNING:
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
                userId, request.getCardId(), card.getType(), rating, card.getReviewDueAt());
    }

    /**
     * TODO
     * 该方法通过一个 for 循环，逐个调用 submitReview 方法。submitReview 内部又包含了 SELECT 和 UPDATE 操作。
     *   - 后果: 如果用户一次性提交了 50 张卡片的复习结果，这个操作会串行地执行 50 次数据库 SELECT 和 50 次数据库 UPDATE。这不仅效率低下，而且由于 try-catch
     *   的存在，如果中间有一次失败，事务不会回滚，导致数据部分更新，可能会让用户感到困惑。
     *   - 修复建议: 必须对这个方法进行批量化重构。
     *     a. 批量查询：在循环开始前，一次性通过 srsStateDataService.getByUserAndCards(userId, cardIds) 获取所有需要处理的 UserCardSrsStateDO，并放入一个 Map。
     *     b. 在内存中计算：在 for 循环中，从 Map 中获取卡片状态，然后调用 calculateSM2 计算出新的状态。将这些需要更新的状态（UserCardSrsStateDO
     *   对象）收集到一个 List 中。
     *     c. 批量更新：循环结束后，调用 srsStateDataService.batchUpdateAfterReview(List<UserCardSrsStateDO> statesToUpdate)。这个新方法需要使用 MyBatis 的
     *   <foreach> 标签实现批量 UPDATE (通常使用 CASE WHEN ... THEN ... 语法)。
     *     d. 错误处理：移除 for 循环中的 try-catch。整个方法由 @Transactional
     *   包裹，任何一次计算或数据库批量更新失败，都会导致整个批次的回滚，保证了数据的一致性。
     * 批量提交复习结果
     */
    @Transactional
    public void batchSubmitReview(Long userId, ReviewSessionRequest session) {
        userDataService.validateExists(userId);
        if (session == null || session.getResults() == null || session.getResults().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception("请求参数不能为空");
        }

        // 逐个处理复习结果
        for (ReviewCardResultDTO result : session.getResults()) {
            ReviewCardRequest request = new ReviewCardRequest();
            request.setCardId(result.getCardId());
            request.setResult(result.getResult());
            request.setTimeSpent(result.getTimeSpent());
            
            try {
                submitReview(userId, request);
            } catch (Exception e) {
                log.warn("Failed to submit review for card: {} user: {}", result.getCardId(), userId, e);
                // 继续处理其他卡片，不因单个失败中断整个批次
            }
        }

        log.info("Batch submitted {} reviews for user: {}", session.getResults().size(), userId);
    }

    /**
     * 获取复习统计
     */
    public ReviewStatsDTO getReviewStats(Long userId, Enums.Period period) {
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (period == null) {
            period = Enums.Period.WEEK;
        }

        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = calculateStartTime(endTime, period);

        // 实现统计查询
        Long totalReviews = srsStateDataService.countReviewsInPeriod(userId, startTime, endTime);
        Integer streakDays = srsStateDataService.calculateStreakDays(userId);
        Double averageScore = srsStateDataService.calculateAverageScore(userId, startTime, endTime);
        Long timeSpent = srsStateDataService.calculateTimeSpent(userId, startTime, endTime);

        return toReviewStatsDTO(totalReviews, streakDays, averageScore, timeSpent);
    }

    // ========== Anki 算法核心实现 ==========

    /**
     * 处理新卡片 (NEW -> LEARNING 或 REVIEW)
     */
    private void handleNewCard(UserCardSrsDO card, int rating) {
        SystemProperties.Srs.Algorithm config = systemProperties.getSrs().getAlgorithm();

        if (rating == 1 || rating == 2) {
            // 评级"重来"(1) 或 "困难"(2): 进入学习流程
            card.setType(TYPE_LEARNING);
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
                card.setType(TYPE_LEARNING);
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
        boolean isRelearning = (card.getType() == TYPE_RELEARNING);

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
            card.setType(TYPE_RELEARNING);
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
        card.setType(TYPE_REVIEW);
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
        card.setType(TYPE_REVIEW);
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
    private LocalDateTime calculateStartTime(LocalDateTime endTime, Enums.Period period) {
        switch (period) {
            case DAY:
                return endTime.minusDays(1);
            case WEEK:
                return endTime.minusDays(7);
            case MONTH:
                return endTime.minusDays(30);
            case YEAR:
                return endTime.minusDays(365);
            default:
                return endTime.minusDays(7);
        }
    }

}