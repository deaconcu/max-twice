package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.UserCardSrsConverter;
import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.response.ReviewStatsDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.review.ReviewDomainService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 复习应用服务
 *
 * 负责协调跨领域逻辑、DTO转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewDomainService reviewDomainService;
    private final MemoryCardDataService cardDataService;
    private final UserDataService userDataService;
    private final UserCardSrsConverter srsStateConverter;
    private final MemoryCardService memoryCardService;


    // ========== toDTO ==========

    /**
     * 转换为复习统计DTO
     */
    public ReviewStatsDTO toReviewStatsDTO(ReviewDomainService.ReviewStats stats) {
        ReviewStatsDTO dto = new ReviewStatsDTO();
        dto.setTotalReviewCount(stats.getTotalReviewCount() != null ? stats.getTotalReviewCount().intValue() : 0);
        dto.setStreakDays(stats.getStreakDays() != null ? stats.getStreakDays() : 0);
        dto.setAverageScore(stats.getAverageScore() != null ? stats.getAverageScore() : 0.0);
        dto.setTimeSpent(stats.getTimeSpent() != null ? stats.getTimeSpent().intValue() : 0);
        return dto;
    }

    // ========== 业务方法 ==========

    /**
     * 获取复习队列
     */
    public List<CardWithSrsDTO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit, Long lastId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        // 委托给 DomainService 查询复习队列
        List<UserCardSrsDO> userCardList = reviewDomainService.getReviewQueue(userId, dueOnly, courseId, limit, lastId);

        if (userCardList.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域查询：获取卡片信息
        Set<Long> cardIds = userCardList.stream()
            .map(UserCardSrsDO::getCardId)
            .collect(Collectors.toSet());

        List<MemoryCardDO> cardList = cardDataService.getByIds(cardIds);

        // 使用 MemoryCardService 转换为 DTO（包含 SRS 信息）
        return memoryCardService.toCardViewWithSrs(cardList, userId);
    }

    /**
     * 提交复习结果 - Anki 算法实现
     */
    @Transactional
    public void submitReview(Long userId, ReviewCardRequest request) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (request == null || request.getCardId() == null) {
            throw StatusCode.INVALID_PARAMETER.exception("请求参数不能为空");
        }

        // 委托给 DomainService 处理复习逻辑
        reviewDomainService.submitReview(userId, request.getCardId(), request.getResult());
    }

    /**
     * 批量提交复习结果
     *
     * @deprecated 此方法未被前端使用，且实现存在严重问题：
     *   1. 性能问题: N+1 查询，50张卡片 = 100次数据库操作
     *   2. 一致性问题: try-catch 吞掉异常，事务无法回滚，导致数据部分更新
     *   3. 无实际需求: 前端已使用单次提交模式 (reviewCard)，体验更好
     *
     * 如需重新启用，必须按以下方式重构:
     *   a. 批量查询: srsStateDataService.getByUserAndCards(userId, cardIds)
     *   b. 内存计算: 在循环中计算新状态，收集到 List
     *   c. 批量更新: srsStateDataService.batchUpdateAfterReview(List<UserCardSrsStateDO>)
     *   d. 错误处理: 移除 try-catch，让 @Transactional 自然回滚
     */
//    @Transactional
//    public void batchSubmitReview(Long userId, ReviewSessionRequest session) {
//        userDataService.validateExists(userId);
//        if (session == null || session.getResults() == null || session.getResults().isEmpty()) {
//            throw ErrorCode.INVALID_PARAMETER.exception("请求参数不能为空");
//        }
//
//        // 逐个处理复习结果
//        for (ReviewCardResultDTO result : session.getResults()) {
//            ReviewCardRequest request = new ReviewCardRequest();
//            request.setCardId(result.getCardId());
//            request.setResult(result.getResult());
//            request.setTimeSpent(result.getTimeSpent());
//
//            try {
//                submitReview(userId, request);
//            } catch (Exception e) {
//                log.warn("Failed to submit review for card: {} user: {}", result.getCardId(), userId, e);
//                // 继续处理其他卡片，不因单个失败中断整个批次
//            }
//        }
//
//        log.info("Batch submitted {} reviews for user: {}", session.getResults().size(), userId);
//    }

    /**
     * 获取复习统计
     */
    public ReviewStatsDTO getReviewStats(Long userId, Period period) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (period == null) {
            period = Period.WEEK;
        }

        // 计算时间范围
        LocalDateTime endTime = TimeZoneUtil.nowDateTime();
        LocalDateTime startTime = reviewDomainService.calculateStartTime(endTime, period.name());

        // 委托给 DomainService 获取统计
        ReviewDomainService.ReviewStats stats = reviewDomainService.getReviewStats(userId, startTime, endTime);

        return toReviewStatsDTO(stats);
    }
}
