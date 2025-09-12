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

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    private final UserCardSrsStateDataService srsStateDataService;
    private final MemoryCardDataService cardDataService;
    private final UserCardSrsStateConverter srsStateConverter;
    private final MemoryCardService memoryCardService;
    private final UserDataService userDataService;

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
    public List<MemoryCardViewDTO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit) {
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        
        // 参数默认值
        if (dueOnly == null) dueOnly = true;
        if (limit == null) limit = 50;
        if (limit > 500) limit = 500;

        List<UserCardSrsStateDO> userCardList;
        
        if (courseId != null) {
            // 按课程筛选
            userCardList = srsStateDataService.getReviewQueueByCourse(userId, courseId, dueOnly, limit);
        } else {
            // 获取所有课程的复习队列
            userCardList = srsStateDataService.getReviewQueue(userId, dueOnly, limit);
        }

        if (userCardList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取卡片信息
        Set<Long> cardIds = userCardList.stream()
            .map(UserCardSrsStateDO::getCardId)
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
        UserCardSrsStateDO srsState = srsStateDataService.getByUserAndCard(userId, request.getCardId());
        if (srsState == null) {
            throw ErrorCode.SRS_STATE_NOT_FOUND.exception();
        }

        // 执行SM-2算法计算
        SM2Result sm2Result = calculateSM2(srsState, request.getResult());

        // 更新SRS状态
        UserCardSrsStateDO updateState = new UserCardSrsStateDO();
        updateState.setId(srsState.getId());
        updateState.setLastReviewedAt(LocalDateTime.now());
        updateState.setIntervalDays(sm2Result.getIntervalDays());
        updateState.setEaseFactor(sm2Result.getEaseFactor());
        updateState.setRepetitions(sm2Result.getRepetitions());
        updateState.setReviewDueAt(sm2Result.getNextReviewDate());
        updateState.setUpdatedAt(LocalDateTime.now());

        // 如果复习失败，增加遗忘次数
        if (request.getResult() < ReviewResult.GOOD.value()) {
            updateState.setLapseCount(srsState.getLapseCount() + 1);
        }

        srsStateDataService.updateAfterReview(
            userId, 
            request.getCardId(), 
            sm2Result.getNextReviewDate(),
            sm2Result.getIntervalDays(), 
            sm2Result.getEaseFactor(), 
            sm2Result.getRepetitions(), 
            updateState.getLapseCount()
        );

        log.info("Updated SRS state for user: {} card: {} result: {} next: {}", 
            userId, request.getCardId(), request.getResult(), sm2Result.getNextReviewDate());
    }

    /**
     * 批量提交复习结果
     */
    @Transactional
    public void batchSubmitReview(Long userId, ReviewSessionDTO session) {
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
     * SM-2算法计算结果
     */
    private static class SM2Result {
        private final int intervalDays;
        private final BigDecimal easeFactor;
        private final int repetitions;
        private final LocalDateTime nextReviewDate;

        public SM2Result(int intervalDays, BigDecimal easeFactor, int repetitions, LocalDateTime nextReviewDate) {
            this.intervalDays = intervalDays;
            this.easeFactor = easeFactor;
            this.repetitions = repetitions;
            this.nextReviewDate = nextReviewDate;
        }

        public int getIntervalDays() { return intervalDays; }
        public BigDecimal getEaseFactor() { return easeFactor; }
        public int getRepetitions() { return repetitions; }
        public LocalDateTime getNextReviewDate() { return nextReviewDate; }
    }

    /**
     * SM-2算法核心实现
     * 
     * @param srsState 当前SRS状态
     * @param quality 复习质量 (0-5: 0=完全忘记, 1=错误重复, 2=错误简单, 3=正确困难, 4=正确, 5=完美)
     * @return SM2计算结果
     */
    private SM2Result calculateSM2(UserCardSrsStateDO srsState, Integer quality) {
        BigDecimal easeFactor = srsState.getEaseFactor();
        int repetitions = srsState.getRepetitions();
        int intervalDays = srsState.getIntervalDays();

        if (quality >= 3) {
            // 回答正确
            if (repetitions == 0) {
                intervalDays = 1;
            } else if (repetitions == 1) {
                intervalDays = 6;
            } else {
                intervalDays = easeFactor.multiply(BigDecimal.valueOf(intervalDays)).intValue();
            }
            repetitions++;
        } else {
            // 回答错误，重置间隔和重复次数
            repetitions = 0;
            intervalDays = 1;
        }

        // 更新难度因子
        BigDecimal qualityFactor = BigDecimal.valueOf(0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        easeFactor = easeFactor.add(qualityFactor);
        
        // 确保难度因子在合理范围内
        if (easeFactor.compareTo(BigDecimal.valueOf(1.3)) < 0) {
            easeFactor = BigDecimal.valueOf(1.3);
        }

        // 计算下次复习时间
        LocalDateTime nextReviewDate = LocalDateTime.now().plusDays(intervalDays);

        return new SM2Result(intervalDays, easeFactor, repetitions, nextReviewDate);
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