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
    private final CourseMemoryBankConverter courseMemoryBankConverter;

    // ========== toDTO ==========

    /**
     * 转换为课程记忆库DTO
     */
    public CourseMemoryBankDTO toCourseMemoryBankDTO(CourseDO courseDO, UserCourseSrsSettingDO settingDO, Long userId) {
        CourseMemoryBankDO courseMemoryBankDO = userCardInCourseDataService.getCardStatsForCourses(userId, courseDO.getId());
        CourseMemoryBankDTO bankDTO = courseMemoryBankConverter.toCardStatsDTO(courseMemoryBankDO);

        bankDTO.setCourse(courseConverter.toDTOV3(courseDO));
        bankDTO.setSetting(courseSrsSettingConverter.toDTO(settingDO));

        return bankDTO;
    }
    
    public List<CourseMemoryBankDTO> toCourseMemoryBankDTO(List<CourseDO> courseDOList, Map<Long, UserCourseSrsSettingDO> settingMap, Long userId) {
        Map<Long, CourseDO> courseMap = courseDOList.stream()
                .collect(Collectors.toMap(CourseDO::getId, course -> course));
        List<CourseMemoryBankDO> bankDOList = userCardInCourseDataService.getBatchCardStatsForCourses(userId, courseMap.keySet());

        List<CourseMemoryBankDTO> bankDTOList = new ArrayList<>();
        for (CourseMemoryBankDO bankDO: bankDOList) {
            CourseMemoryBankDTO courseMemoryBankDTO = courseMemoryBankConverter.toCardStatsDTO(bankDO);
            courseMemoryBankDTO.setCourse(courseConverter.toDTOV3(courseMap.get(bankDO.getCourseId())));
            courseMemoryBankDTO.setSetting(courseSrsSettingConverter.toDTO(settingMap.get(bankDO.getCourseId())));
            bankDTOList.add(courseMemoryBankDTO);
        }
        return bankDTOList;
    }


    // ========== 业务方法(Query) ==========

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


    // ========== 业务方法(Command) ==========

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
            
            // 批量添加卡片到课程
            List<Long> cardIds = cards.stream()
                    .map(MemoryCardDO::getId)
                    .collect(Collectors.toList());
            
            // 批量插入用户卡片课程关系（自动跳过已存在的）
            userCardInCourseDataService.batchInsertIgnore(userId, request.getCourseId(), cardIds);
            
            // 批量创建SRS状态（自动跳过已存在的）
            userCardSrsStateDataService.batchInsertIgnoreSrsStates(userId, cardIds);
            
        } else {
            log.info("No cards found in deck: {} to add to memory bank for user: {} in course: {}", 
                request.getDeckId(), userId, request.getCourseId());
        }
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

        // 直接修改已获取的设置对象，避免重复查询
        if (request.getFrequencySetting() != null) {
            existingSetting.setFrequencySetting(request.getFrequencySetting().byteValue());
        }
        if (request.getStatus() != null) {
            existingSetting.setStatus(request.getStatus().byteValue());
        }
        existingSetting.setUpdatedAt(LocalDateTime.now());
        courseSrsSettingDataService.update(existingSetting);

        log.info("Updated course setting for user: {} course: {}", userId, courseId);

        // TODO
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
            List<Long> cardIds = cards.stream()
                    .map(MemoryCardDO::getId)
                    .collect(Collectors.toList());
            
            // 批量删除用户卡片课程关系
            userCardInCourseDataService.batchDeleteByUserCourseAndCards(userId, courseId, cardIds);
            
            // 批量查询仍有课程关系的卡片ID（优化1+N查询）
            List<Long> existingCardIds = userCardInCourseDataService.getExistingCardIdsByUserAndCards(userId, cardIds);
            Set<Long> existingCardSet = new HashSet<>(existingCardIds);
            
            // 找出孤立的卡片ID（没有其他课程关系的卡片）
            List<Long> orphanedCardIds = cardIds.stream()
                    .filter(cardId -> !existingCardSet.contains(cardId))
                    .collect(Collectors.toList());
            
            // 批量删除孤立卡片的SRS状态
            if (!orphanedCardIds.isEmpty()) {
                userCardSrsStateDataService.batchDeleteByUserAndCards(userId, orphanedCardIds);
            }
            
            log.info("Removed {} cards from deck: {} from course: {} for user: {}, deleted {} orphaned SRS states", 
                cards.size(), deckId, courseId, userId, orphanedCardIds.size());
        }

        log.info("Removed deck: {} from course: {} for user: {}", deckId, courseId, userId);
    }

}