package com.prosper.learn.domain.service.business;

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

import static com.google.common.base.Preconditions.*;

import static com.prosper.learn.common.Enums.*;

/**
 * 记忆库管理业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryBankService {

    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDataService cardDataService;
    private final UserCourseSrsSettingDataService courseSrsSettingDataService;
    private final UserCardInCourseDataService userCardInCourseDataService;
    private final UserCardSrsStateDataService userCardSrsStateDataService;
    private final CourseDataService courseDataService;
    private final UserDataService userDataService;
    private final PostDataService postDataService;
    private final CourseConverter courseConverter;
    private final UserCourseSrsSettingConverter courseSrsSettingConverter;

    // ========== toDTO ==========

    /**
     * 转换为课程记忆库DTO
     */
    public CourseMemoryBankDTO toCourseMemoryBankDTO(CourseDO courseDO, UserCourseSrsSettingDO settingDO, Long userId) {
        if (courseDO == null) return null;
        
        CourseMemoryBankDTO dto = new CourseMemoryBankDTO();
        dto.setCourse(courseConverter.toDTOV3(courseDO));
        
        if (settingDO != null) {
            dto.setSetting(courseSrsSettingConverter.toDTO(settingDO));
        }
        
        // 获取卡片统计信息
        if (userId != null) {
            Long totalCardCount = userCardInCourseDataService.countCardsByUserAndCourse(userId, courseDO.getId());
            Long dueCardCount = userCardSrsStateDataService.countDueCardsByUserAndCourse(userId, courseDO.getId());
            Long newCardCount = userCardSrsStateDataService.countNewCardsByUserAndCourse(userId, courseDO.getId());
            Long reviewCardCount = userCardSrsStateDataService.countReviewCardsByUserAndCourse(userId, courseDO.getId());
            Long learnedCardCount = userCardSrsStateDataService.countLearnedCardsByUserAndCourse(userId, courseDO.getId());
            
            dto.setCardCount(totalCardCount != null ? totalCardCount.intValue() : 0);
            dto.setDueCardCount(dueCardCount != null ? dueCardCount.intValue() : 0);
            dto.setNewCardCount(newCardCount != null ? newCardCount.intValue() : 0);
            dto.setReviewCardCount(reviewCardCount != null ? reviewCardCount.intValue() : 0);
            dto.setLearnedCardCount(learnedCardCount != null ? learnedCardCount.intValue() : 0);
        } else {
            dto.setCardCount(0);
            dto.setDueCardCount(0);
            dto.setNewCardCount(0);
            dto.setReviewCardCount(0);
            dto.setLearnedCardCount(0);
        }
        
        return dto;
    }
    
    public CourseMemoryBankDTO toCourseMemoryBankDTO(CourseDO courseDO, UserCourseSrsSettingDO settingDO) {
        return toCourseMemoryBankDTO(courseDO, settingDO, null);
    }

    public List<CourseMemoryBankDTO> toCourseMemoryBankDTO(List<CourseDO> courseDOList, Map<Long, UserCourseSrsSettingDO> settingMap, Long userId) {
        if (courseDOList == null || courseDOList.isEmpty()) {
            return new ArrayList<>();
        }
        
        return courseDOList.stream()
            .map(course -> {
                UserCourseSrsSettingDO setting = settingMap.get(course.getId());
                return toCourseMemoryBankDTO(course, setting, userId);
            })
            .collect(Collectors.toList());
    }
    
    public List<CourseMemoryBankDTO> toCourseMemoryBankDTO(List<CourseDO> courseDOList, Map<Long, UserCourseSrsSettingDO> settingMap) {
        return toCourseMemoryBankDTO(courseDOList, settingMap, null);
    }

    // ========== 业务方法 ==========

    /**
     * 添加卡片组到记忆库
     */
    @Transactional
    public void addDeckToMemoryBank(Long userId, AddDeckToMemoryBankRequest request) {
        // 验证request参数
        checkNotNull(request);

        // 直接使用validateAndGet验证并获取实体
        UserDO user = userDataService.validateAndGet(userId);
        MemoryCardDeckDO deck = deckDataService.validateAndGet(request.getDeckId());
        CourseDO course = courseDataService.validateAndGet(request.getCourseId());

        // 创建或更新课程学习设置
        UserCourseSrsSettingDO existingSetting = courseSrsSettingDataService.getByUserAndCourse(userId, request.getCourseId());
        if (existingSetting == null) {
            // 创建新的课程设置
            UserCourseSrsSettingDO setting = new UserCourseSrsSettingDO();
            setting.setUserId(userId);
            setting.setCourseId(request.getCourseId());
            setting.setFrequencySetting(FrequencySetting.NORMAL.value()); // 默认普通频率
            setting.setStatus(CourseStudyStatus.STUDYING.value()); // 学习中
            setting.setCreatedAt(LocalDateTime.now());
            setting.setUpdatedAt(LocalDateTime.now());
            courseSrsSettingDataService.insert(setting);
        }

        // 获取卡片组中的所有卡片
        List<MemoryCardDO> cards = cardDataService.getByDeckId(request.getDeckId());
        
        if (cards != null && !cards.isEmpty()) {
            log.info("Adding {} cards from deck: {} to memory bank for user: {} in course: {}", 
                cards.size(), request.getDeckId(), userId, request.getCourseId());
            
            // 实际的卡片添加逻辑
            for (MemoryCardDO card : cards) {
                // 添加卡片到课程
                addCardToCourse(userId, card.getId(), request.getCourseId());
            }
        } else {
            log.info("No cards found in deck: {} to add to memory bank for user: {} in course: {}", 
                request.getDeckId(), userId, request.getCourseId());
        }
    }

    /**
     * 获取记忆库课程列表
     */
    public List<CourseMemoryBankDTO> getMemoryBankCourses(Long userId, Integer state) {
        // 验证用户存在性
        userDataService.validateExists(userId);

        // 获取用户的课程设置列表
        List<UserCourseSrsSettingDO> settings = courseSrsSettingDataService.getByUser(userId);
        if (settings.isEmpty()) {
            return new ArrayList<>();
        }

        // 过滤状态
        if (state != null) {
            settings = settings.stream()
                .filter(setting -> setting.getStatus().equals(state.byteValue()))
                .collect(Collectors.toList());
        }

        // 获取课程信息
        Set<Long> courseIds = settings.stream()
            .map(UserCourseSrsSettingDO::getCourseId)
            .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(courseIds);

        // 构建设置映射
        Map<Long, UserCourseSrsSettingDO> settingMap = settings.stream()
            .collect(Collectors.toMap(UserCourseSrsSettingDO::getCourseId, s -> s));

        // 转换为DTO
        List<CourseDO> courses = settings.stream()
            .map(setting -> courseMap.get(setting.getCourseId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return toCourseMemoryBankDTO(courses, settingMap, userId);
    }

    /**
     * 更新课程复习策略
     */
    @Transactional
    public void updateCourseSetting(Long userId, Long courseId, UpdateCourseSettingRequest request) {
        // 验证参数
        checkNotNull(request);
        
        // 验证存在性
        userDataService.validateExists(userId);
        courseDataService.validateExists(courseId);

        // 获取现有设置
        UserCourseSrsSettingDO existingSetting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (existingSetting == null) {
            throw ErrorCode.MEMORY_BANK_COURSE_NOT_FOUND.exception("课程设置不存在");
        }

        // 更新设置
        UserCourseSrsSettingDO setting = new UserCourseSrsSettingDO();
        setting.setId(existingSetting.getId());
        if (request.getFrequencySetting() != null) {
            setting.setFrequencySetting(request.getFrequencySetting().byteValue());
        }
        if (request.getStatus() != null) {
            setting.setStatus(request.getStatus().byteValue());
        }
        setting.setUpdatedAt(LocalDateTime.now());

        courseSrsSettingDataService.update(setting);

        log.info("Updated course setting for user: {} course: {}", userId, courseId);

        // 触发异步任务重新计算复习时间
        // 这里可以发送异步事件或调用异步服务
        // asyncRecalculationService.recalculateDueDates(userId, courseId);
        // 或者使用Spring的@Async注解创建异步方法
        // 暂时跳过异步任务，如需要可后续实现
    }

    /**
     * 移除课程中的卡片组
     */
    @Transactional
    public void removeDeckFromCourse(Long userId, Long courseId, Long deckId) {
        // 验证存在性 - 统一使用validateExists
        userDataService.validateExists(userId);
        courseDataService.validateExists(courseId);
        deckDataService.validateExists(deckId);

        // 验证业务逻辑 - 课程设置存在
        UserCourseSrsSettingDO courseSetting = courseSrsSettingDataService.getByUserAndCourse(userId, courseId);
        if (courseSetting == null) {
            throw ErrorCode.MEMORY_BANK_COURSE_NOT_FOUND.exception("课程设置不存在");
        }

        // 获取卡片组中的所有卡片并移除
        List<MemoryCardDO> cards = cardDataService.getByDeckId(deckId);
        if (cards != null && !cards.isEmpty()) {
            for (MemoryCardDO card : cards) {
                removeCardFromCourse(userId, card.getId(), courseId);
            }
            log.info("Removed {} cards from deck: {} from course: {} for user: {}", 
                cards.size(), deckId, courseId, userId);
        }

        log.info("Removed deck: {} from course: {} for user: {}", deckId, courseId, userId);
    }

    // ========== 私有方法 ==========

    /**
     * 添加卡片到课程
     */
    private void addCardToCourse(Long userId, Long cardId, Long courseId) {
        // 检查卡片是否已经在课程中
        UserCardInCourseDO existing = userCardInCourseDataService.getByUserCardAndCourse(userId, cardId, courseId);
        if (existing != null) {
            return; // 已存在，跳过
        }

        // 添加卡片到课程
        UserCardInCourseDO cardInCourse = new UserCardInCourseDO();
        cardInCourse.setUserId(userId);
        cardInCourse.setCardId(cardId);
        cardInCourse.setCourseId(courseId);
        cardInCourse.setCreatedAt(LocalDateTime.now());
        userCardInCourseDataService.insert(cardInCourse);

        // 检查并创建SRS状态
        UserCardSrsStateDO existingSrsState = userCardSrsStateDataService.getByUserAndCard(userId, cardId);
        if (existingSrsState == null) {
            UserCardSrsStateDO srsState = new UserCardSrsStateDO();
            srsState.setUserId(userId);
            srsState.setCardId(cardId);
            srsState.setDeckVersion(1);
            // 获取实际的卡片版本ID
            MemoryCardDO cardInfo = cardDataService.getById(cardId);
            Long versionId = cardInfo != null && cardInfo.getCurrentVersionId() != null 
                ? cardInfo.getCurrentVersionId() : 1L;
            srsState.setCardVersionId(versionId);
            srsState.setReviewDueAt(LocalDateTime.now()); // 立即可复习
            srsState.setIntervalDays(0);
            srsState.setEaseFactor(new BigDecimal(2.5));
            srsState.setRepetitions(0);
            srsState.setLapseCount(0);
            srsState.setCreatedAt(LocalDateTime.now());
            srsState.setUpdatedAt(LocalDateTime.now());
            userCardSrsStateDataService.insert(srsState);
        }
    }

    /**
     * 从课程中移除卡片
     */
    private void removeCardFromCourse(Long userId, Long cardId, Long courseId) {
        UserCardInCourseDO cardInCourse = userCardInCourseDataService.getByUserCardAndCourse(userId, cardId, courseId);
        if (cardInCourse != null) {
            userCardInCourseDataService.deleteById(cardInCourse.getId());
        }

        // 检查卡片是否还在其他课程中，如果不在则删除SRS状态
        List<UserCardInCourseDO> otherCourses = userCardInCourseDataService.getByUserAndCard(userId, cardId);
        if (otherCourses == null || otherCourses.isEmpty()) {
            // 卡片不在任何课程中，删除SRS状态
            userCardSrsStateDataService.deleteByUserAndCard(userId, cardId);
        }
    }

}