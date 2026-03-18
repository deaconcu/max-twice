package com.prosper.learn.application.service;

import com.prosper.learn.application.assembler.CardAssembler;
import com.prosper.learn.application.dto.request.ReviewCardRequest;
import com.prosper.learn.application.dto.response.CourseMemoryBankDTO;
import com.prosper.learn.application.dto.response.ReviewSubmitResultDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.review.ReviewDomainService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.memory.review.UserCourseSrsSettingDO;
import com.prosper.learn.memory.review.UserCourseSrsSettingDataService;
import com.prosper.learn.shared.domain.Enums.CardOrder;
import com.prosper.learn.shared.domain.event.user.review.CardReviewedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final UserCourseSrsSettingDataService courseSettingDataService;
    private final UserCardSrsDataService srsDataService;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemProperties systemProperties;
    private final MemoryBankService memoryBankService;

    // DTO 组装器
    private final CardAssembler cardAssembler;

    // ========== 业务方法 ==========

    /**
     * 获取下一张待复习卡片
     *
     * @param userId 用户ID
     * @param courseId 课程ID（必须指定）
     * @return 下一张卡片，无卡片时返回空结果
     */
    public ReviewSubmitResultDTO getNextCard(Long userId, Long courseId) {
        if (userId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }
        if (courseId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("必须指定课程");
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

        CardWithSrsDTO cardDto = cardAssembler.toCardViewWithSrs(card, userId);
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
        if (request.getCourseId() == null) {
            throw StatusCode.INVALID_PARAMETER.exception("必须指定课程");
        }

        Long cardId = request.getCardId();
        int rating = request.getResult();
        Long courseId = request.getCourseId();

        // 1. 递增用户的复习卡片计数
        long reviewCardCount = userDataService.incrementReviewCardCount(userId);

        // 2. 处理 SRS 算法
        reviewDomainService.submitReview(userId, cardId, courseId, rating, reviewCardCount);

        // 3. 发布复习完成事件（用于更新连续复习天数等统计）
        LocalDate userToday = getUserToday(userId);
        eventPublisher.publishEvent(new CardReviewedEvent(userId, cardId, rating, userToday));

        // 4. 获取卡片顺序设置
        boolean newFirst = getNewFirst(userId, courseId);

        // 5. 获取下一张卡片
        UserCardSrsDO nextSrs = reviewDomainService.getNextCard(userId, courseId, reviewCardCount, newFirst);

        ReviewSubmitResultDTO result;
        if (nextSrs == null) {
            result = ReviewSubmitResultDTO.empty();
        } else {
            MemoryCardDO nextCard = cardDataService.getById(nextSrs.getCardId());
            if (nextCard == null) {
                result = ReviewSubmitResultDTO.empty();
            } else {
                CardWithSrsDTO nextCardDto = cardAssembler.toCardViewWithSrs(nextCard, userId);
                result = ReviewSubmitResultDTO.of(nextCardDto);
            }
        }

        // 6. 查询当前课程统计并填入返回结果
        CourseMemoryBankDTO courseStats = memoryBankService.getSingleCourseStat(userId, courseId);
        result.setCourseStats(courseStats);

        return result;
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
            srsList = srsDataService.getCardListByCourse(userId, courseId, limit, lastId);
        } else {
            srsList = srsDataService.getCardList(userId, limit, lastId);
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
            CardWithSrsDTO dto = cardAssembler.toCardViewWithSrs(card, userId);
            result.add(dto);
        }

        return result;
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

    /**
     * 获取用户时区的"今天"日期
     */
    private LocalDate getUserToday(Long userId) {
        try {
            UserDO user = userDataService.getById(userId);
            if (user != null && user.getTimezone() != null && !user.getTimezone().isEmpty()) {
                ZoneId userZone = ZoneId.of(user.getTimezone());
                return LocalDate.now(userZone);
            }
        } catch (Exception e) {
            log.warn("获取用户时区失败，使用默认时区: userId={}", userId, e);
        }
        return LocalDate.now(ZoneId.of(systemProperties.getUser().getDefaultTimezone()));
    }
}
