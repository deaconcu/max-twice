package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.*;
import com.prosper.learn.domain.util.converter.*;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.persistence.dataobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.*;

/**
 * 复习功能业务服务
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

    // SM-2 算法常量
    private static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.3");
    private static final int INITIAL_INTERVAL_DAYS = 1;
    private static final int SECOND_REVIEW_INTERVAL_DAYS = 6;
    private static final int CORRECT_REVIEW_QUALITY_THRESHOLD = 3;


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
    public List<MemoryCardViewDTO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit, Long lastId) {
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
        return memoryCardService.toDTOV1(cardList, userId);
    }

    /**
     * 提交复习结果 - SM-2算法实现
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
        UserCardSrsDO srsState = srsStateDataService.getByUserAndCard(userId, request.getCardId());
        if (srsState == null) {
            throw ErrorCode.SRS_STATE_NOT_FOUND.exception();
        }

        // 执行SM-2算法计算
        SM2Result sm2Result = calculateSM2(srsState, request.getResult());

        // 直接修改查询出来的对象
        srsState.setLastReviewedAt(LocalDateTime.now());
        srsState.setIntervalDays(sm2Result.intervalDays());
        srsState.setEaseFactor(sm2Result.easeFactor());
        srsState.setRepetitions(sm2Result.repetitions());
        srsState.setReviewDueAt(sm2Result.nextReviewDate());
        srsState.setUpdatedAt(LocalDateTime.now());

        // 如果复习失败，增加遗忘次数
        if (request.getResult() < ReviewResult.GOOD.value()) {
            srsState.setLapseCount(srsState.getLapseCount() + 1);
        }

        // 更新修改后的对象到数据库
        srsStateDataService.update(srsState);

        log.info("Updated SRS state for user: {} card: {} result: {} next: {}", 
            userId, request.getCardId(), request.getResult(), sm2Result.nextReviewDate());
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

    // ========== SM-2算法实现 ==========

    /**
     * SM-2算法计算结果的只读数据载体
     *
     * @param intervalDays   新的复习间隔天数
     * @param easeFactor     新的缓急因子
     * @param repetitions    新的连续正确次数
     * @param nextReviewDate 计算出的下次复习日期
     */
    private record SM2Result(
            int intervalDays,
            BigDecimal easeFactor,
            int repetitions,
            LocalDateTime nextReviewDate
    ) {}

    /**
     * SM-2算法核心实现
     *
     * @param srsState 当前SRS状态
     * @param quality  复习质量 (0-5: 0=完全忘记, 1=错误重复, 2=错误简单, 3=正确困难, 4=正确, 5=完美)
     * @return SM2计算结果
     */
    private SM2Result calculateSM2(UserCardSrsDO srsState, Integer quality) {
        BigDecimal currentEaseFactor = srsState.getEaseFactor();
        int currentRepetitions = srsState.getRepetitions();
        int currentIntervalDays = srsState.getIntervalDays();

        int newRepetitions;
        int newIntervalDays;

        if (quality >= CORRECT_REVIEW_QUALITY_THRESHOLD) {
            // 回答正确
            newRepetitions = currentRepetitions + 1;
            if (currentRepetitions == 0) {
                newIntervalDays = INITIAL_INTERVAL_DAYS;
            } else if (currentRepetitions == 1) {
                newIntervalDays = SECOND_REVIEW_INTERVAL_DAYS;
            } else {
                newIntervalDays = currentEaseFactor.multiply(BigDecimal.valueOf(currentIntervalDays)).intValue();
            }
        } else {
            // 回答错误，重置间隔和重复次数
            newRepetitions = 0;
            newIntervalDays = INITIAL_INTERVAL_DAYS;
        }

        // 更新难度因子
        // qd = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
        BigDecimal qualityDelta = BigDecimal.valueOf(0.1)
                .subtract(BigDecimal.valueOf(5 - quality)
                        .multiply(BigDecimal.valueOf(0.08)
                                .add(BigDecimal.valueOf(5 - quality).multiply(BigDecimal.valueOf(0.02)))));

        BigDecimal newEaseFactor = currentEaseFactor.add(qualityDelta);

        // 确保难度因子不低于最小值
        if (newEaseFactor.compareTo(MIN_EASE_FACTOR) < 0) {
            newEaseFactor = MIN_EASE_FACTOR;
        }

        // 计算下次复习时间
        LocalDateTime nextReviewDate = LocalDateTime.now().plusDays(newIntervalDays);

        return new SM2Result(newIntervalDays, newEaseFactor, newRepetitions, nextReviewDate);
    }

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