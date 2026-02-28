package com.prosper.learn.application.service;

import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.response.ReviewStatsDTO;
import com.prosper.learn.application.dto.response.ReviewSubmitResultDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.review.ReviewDomainService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.memory.review.UserCourseSrsSettingDO;
import com.prosper.learn.memory.review.UserCourseSrsSettingDataService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums.CardOrder;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final MemoryCardService memoryCardService;
    private final UserCourseSrsSettingDataService courseSettingDataService;
    private final UserCardSrsDataService srsDataService;

    // ========== 业务方法 ==========

    /**
     * 获取下一张待复习卡片
     *
     * @param userId 用户ID
     * @param courseId 课程ID（可选，null 表示全部课程）
     * @return 下一张卡片，无卡片时返回空结果
     */
    public ReviewSubmitResultDTO getNextCard(Long userId, Long courseId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        // 获取用户的复习卡片计数
        UserDO user = userDataService.validateAndGet(userId);
        long reviewCardCount = user.getReviewCardCount() != null ? user.getReviewCardCount() : 0L;

        // 获取卡片顺序设置
        boolean newFirst = getNewFirst(userId, courseId);

        // 获取下一张卡片
        UserCardSrsDO nextSrs = reviewDomainService.getNextCard(userId, courseId, reviewCardCount, newFirst);
        if (nextSrs == null) {
            return ReviewSubmitResultDTO.empty();
        }

        // 加载卡片完整信息
        MemoryCardDO card = cardDataService.getById(nextSrs.getCardId());
        if (card == null) {
            return ReviewSubmitResultDTO.empty();
        }

        CardWithSrsDTO cardDto = memoryCardService.toCardViewWithSrs(card, userId);
        return ReviewSubmitResultDTO.of(cardDto);
    }

    /**
     * 提交复习结果并返回下一张卡片
     *
     * @param userId 用户ID
     * @param request 复习请求（包含 cardId, result, courseId）
     * @return 下一张卡片
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
        Long courseId = request.getCourseId();

        // 1. 递增用户的复习卡片计数
        long reviewCardCount = userDataService.incrementReviewCardCount(userId);

        // 2. 处理 SRS 算法
        reviewDomainService.submitReview(userId, cardId, rating, reviewCardCount);

        // 3. 获取卡片顺序设置
        boolean newFirst = getNewFirst(userId, courseId);

        // 4. 获取下一张卡片
        UserCardSrsDO nextSrs = reviewDomainService.getNextCard(userId, courseId, reviewCardCount, newFirst);
        if (nextSrs == null) {
            return ReviewSubmitResultDTO.empty();
        }

        // 加载卡片完整信息
        MemoryCardDO nextCard = cardDataService.getById(nextSrs.getCardId());
        if (nextCard == null) {
            return ReviewSubmitResultDTO.empty();
        }

        CardWithSrsDTO nextCardDto = memoryCardService.toCardViewWithSrs(nextCard, userId);
        return ReviewSubmitResultDTO.of(nextCardDto);
    }

    /**
     * 获取卡片列表（管理界面用）
     *
     * @param userId 用户ID
     * @param courseId 课程ID（可选，null 表示全部课程）
     * @param lastId 分页游标
     * @return 卡片列表
     */
    public List<CardWithSrsDTO> getCardList(Long userId, Long courseId, Long lastId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        int limit = 20;

        // 获取卡片 SRS 列表
        List<UserCardSrsDO> srsList;
        if (courseId != null) {
            srsList = srsDataService.getReviewQueueByCourse(userId, courseId, false, limit, lastId);
        } else {
            srsList = srsDataService.getReviewQueue(userId, false, limit, lastId);
        }

        if (srsList.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取卡片 ID 列表
        Set<Long> cardIds = srsList.stream()
                .map(UserCardSrsDO::getCardId)
                .collect(Collectors.toSet());

        // 批量获取卡片信息
        List<MemoryCardDO> cards = cardDataService.getByIds(cardIds);

        // 转换为 DTO
        List<CardWithSrsDTO> result = new ArrayList<>();
        for (MemoryCardDO card : cards) {
            CardWithSrsDTO dto = memoryCardService.toCardViewWithSrs(card, userId);
            result.add(dto);
        }

        return result;
    }

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

    // ========== 私有方法 ==========

    /**
     * 获取卡片顺序设置
     *
     * @return true=先新卡后复习，false=先复习后新卡（默认）
     */
    private boolean getNewFirst(Long userId, Long courseId) {
        if (courseId == null) {
            // 全部课程模式，使用默认设置（先复习后新卡）
            return false;
        }

        UserCourseSrsSettingDO setting = courseSettingDataService.getByUserAndCourse(userId, courseId);
        if (setting == null || setting.getCardOrder() == null) {
            return false;
        }

        return setting.getCardOrder() == CardOrder.NEW_FIRST.value();
    }

    private ReviewStatsDTO toReviewStatsDTO(ReviewDomainService.ReviewStats stats) {
        ReviewStatsDTO dto = new ReviewStatsDTO();
        dto.setTotalReviewCount(stats.getTotalReviewCount() != null ? stats.getTotalReviewCount().intValue() : 0);
        dto.setStreakDays(stats.getStreakDays() != null ? stats.getStreakDays() : 0);
        dto.setAverageScore(stats.getAverageScore() != null ? stats.getAverageScore() : 0.0);
        dto.setTimeSpent(stats.getTimeSpent() != null ? stats.getTimeSpent().intValue() : 0);
        return dto;
    }
}
