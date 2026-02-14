package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.UserCardSrsConverter;
import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.response.ReviewStatsDTO;
import com.prosper.learn.application.dto.response.ReviewSubmitResultDTO;
import com.prosper.learn.application.dto.response.UserCardSrsDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.review.LearningQueueService;
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
    private final LearningQueueService learningQueueService;


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
     *
     * 优先从 Redis 加载队列顺序（LEARNING/RELEARNING 阶段的交错复习支持）
     * 如果 Redis 中没有队列，则按原规则查询
     */
    public List<CardWithSrsDTO> getReviewQueue(Long userId, Boolean dueOnly, Long courseId, Integer limit, Long lastId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        // 首次加载（无 lastId）时，尝试从 Redis 获取队列顺序
        if (lastId == null && dueOnly) {
            List<Long> cachedQueue = learningQueueService.getQueue(userId);
            if (!cachedQueue.isEmpty()) {
                // 验证并过滤队列中的卡片（只保留 LEARNING/RELEARNING 状态）
                List<Long> validQueue = learningQueueService.validateAndFilterQueue(userId, cachedQueue);
                if (!validQueue.isEmpty()) {
                    // 按队列顺序加载卡片
                    List<MemoryCardDO> cardList = cardDataService.getByIds(new HashSet<>(validQueue));

                    // 使用 MemoryCardService 转换为 DTO
                    List<CardWithSrsDTO> result = memoryCardService.toCardViewWithSrs(cardList, userId);

                    // 按队列顺序排序
                    Map<Long, CardWithSrsDTO> cardMap = result.stream()
                            .collect(Collectors.toMap(CardWithSrsDTO::getId, c -> c));

                    List<CardWithSrsDTO> orderedResult = validQueue.stream()
                            .map(cardMap::get)
                            .filter(c -> c != null)
                            .collect(Collectors.toList());

                    // 如果队列有变化，更新 Redis
                    if (validQueue.size() != cachedQueue.size()) {
                        learningQueueService.saveQueue(userId, validQueue);
                    }

                    return orderedResult;
                }
            }
        }

        // 委托给 DomainService 查询复习队列（原逻辑）
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
     * 提交复习结果
     *
     * 后端维护队列，返回下一张卡片
     *
     * @return 下一张卡片和队列信息
     */
    @Transactional
    public ReviewSubmitResultDTO submitReview(Long userId, ReviewCardRequest request) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (request == null || request.getCardId() == null) {
            throw StatusCode.INVALID_PARAMETER.exception("请求参数不能为空");
        }

        Long cardId = request.getCardId();
        int rating = request.getResult();

        // 1. 处理 SRS 算法，返回更新后的状态
        UserCardSrsDO updatedSrs = reviewDomainService.submitReview(userId, cardId, rating);

        // 2. 根据新状态处理队列
        boolean graduated = (updatedSrs.getType() == UserCardSrsDO.TYPE_REVIEW);

        if (graduated) {
            // 毕业：从队列移除
            learningQueueService.removeFromQueue(userId, cardId);
        } else {
            // 未毕业：重排队列
            learningQueueService.reorderQueue(userId, cardId, rating);
        }

        // 3. 检查是否需要加载更多卡片
        if (learningQueueService.needLoadMore(userId)) {
            loadMoreCardsToQueue(userId);
        }

        // 4. 获取下一张卡片
        Long nextCardId = learningQueueService.getFirstCardId(userId);
        int queueSize = learningQueueService.getQueueSize(userId);

        if (nextCardId == null) {
            return ReviewSubmitResultDTO.empty();
        }

        // 加载下一张卡片的完整信息
        MemoryCardDO nextCard = cardDataService.getById(nextCardId);
        if (nextCard == null) {
            return ReviewSubmitResultDTO.empty();
        }

        CardWithSrsDTO nextCardDto = memoryCardService.toCardViewWithSrs(nextCard, userId);

        return ReviewSubmitResultDTO.of(nextCardDto, queueSize, 1);
    }

    /**
     * 加载更多卡片到队列
     */
    private void loadMoreCardsToQueue(Long userId) {
        // 获取当前队列中的卡片ID
        List<Long> existingIds = learningQueueService.getQueue(userId);

        // 每次加载 LOAD_THRESHOLD 张
        int loadCount = LearningQueueService.LOAD_THRESHOLD;

        // 从数据库查询更多到期卡片，排除已在队列中的
        List<UserCardSrsDO> moreCards = reviewDomainService.getReviewQueueExcluding(
            userId, existingIds, loadCount
        );

        if (!moreCards.isEmpty()) {
            List<Long> newCardIds = moreCards.stream()
                .map(UserCardSrsDO::getCardId)
                .collect(Collectors.toList());
            learningQueueService.loadMore(userId, newCardIds);
        }
    }

    /**
     * 获取当前待复习卡片（开始复习时调用）
     *
     * 初始化队列并返回第一张卡片
     */
    public ReviewSubmitResultDTO getCurrentCard(Long userId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        // 检查队列是否为空或需要初始化
        List<Long> queue = learningQueueService.getQueue(userId);
        if (queue.isEmpty()) {
            // 从数据库加载到期卡片初始化队列
            List<UserCardSrsDO> dueCards = reviewDomainService.getReviewQueue(
                userId, true, null, LearningQueueService.LOAD_THRESHOLD, null
            );

            if (dueCards.isEmpty()) {
                return ReviewSubmitResultDTO.empty();
            }

            List<Long> cardIds = dueCards.stream()
                .map(UserCardSrsDO::getCardId)
                .collect(Collectors.toList());
            learningQueueService.initQueue(userId, cardIds);
        }

        // 获取第一张卡片
        Long firstCardId = learningQueueService.getFirstCardId(userId);
        int queueSize = learningQueueService.getQueueSize(userId);

        if (firstCardId == null) {
            return ReviewSubmitResultDTO.empty();
        }

        MemoryCardDO card = cardDataService.getById(firstCardId);
        if (card == null) {
            return ReviewSubmitResultDTO.empty();
        }

        CardWithSrsDTO cardDto = memoryCardService.toCardViewWithSrs(card, userId);
        return ReviewSubmitResultDTO.of(cardDto, queueSize, 1);
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
