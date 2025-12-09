package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.UserCardSrsConverter;
import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.request.ReviewSessionRequest;
import com.prosper.learn.application.dto.response.ReviewCardResultDTO;
import com.prosper.learn.application.dto.response.ReviewStatsDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.review.ReviewDomainService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.shared.domain.exception.ErrorCode;
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
        dto.setTotalReviews(stats.getTotalReviews() != null ? stats.getTotalReviews().intValue() : 0);
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
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
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
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (request == null || request.getCardId() == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("请求参数不能为空");
        }

        // 委托给 DomainService 处理复习逻辑
        reviewDomainService.submitReview(userId, request.getCardId(), request.getResult());
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
    public ReviewStatsDTO getReviewStats(Long userId, Period period) {
        if (userId == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (period == null) {
            period = Period.WEEK;
        }

        // 计算时间范围
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = reviewDomainService.calculateStartTime(endTime, period.name());

        // 委托给 DomainService 获取统计
        ReviewDomainService.ReviewStats stats = reviewDomainService.getReviewStats(userId, startTime, endTime);

        return toReviewStatsDTO(stats);
    }
}
