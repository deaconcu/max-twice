package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.CourseMemoryBankConverter;
import com.prosper.learn.application.converter.UserCourseSrsSettingConverter;
import com.prosper.learn.application.dto.request.AddDeckToMemoryBankRequest;
import com.prosper.learn.application.dto.request.UpdateCourseSettingRequest;
import com.prosper.learn.application.dto.response.CourseMemoryBankDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.memory.bank.MemoryBankDomainService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.*;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 记忆库应用服务
 *
 * 负责协调跨域逻辑、DTO转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryBankService {

    private final MemoryBankDomainService domainService;
    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDataService cardDataService;
    private final CourseDataService courseDataService;
    private final UserDataService userDataService;
    private final PostDataService postDataService;
    private final CourseConverter courseConverter;
    private final UserCourseSrsSettingConverter courseSrsSettingConverter;
    private final CourseMemoryBankConverter courseMemoryBankConverter;

    // ========== DTO 转换方法 ==========

    /**
     * 转换为课程记忆库DTO
     */
    public CourseMemoryBankDTO toCourseMemoryBankDTO(CourseDO courseDO, UserCourseSrsSettingDO settingDO, Long userId) {
        CourseMemoryBankDO courseMemoryBankDO = domainService.getCourseCardStats(userId, courseDO.getId());
        CourseMemoryBankDTO bankDTO = courseMemoryBankConverter.toCardStatsDTO(courseMemoryBankDO);

        bankDTO.setCourse(courseConverter.toBriefDTO(courseDO));
        bankDTO.setSetting(courseSrsSettingConverter.toDTO(settingDO));

        return bankDTO;
    }

    public List<CourseMemoryBankDTO> toCourseMemoryBankDTO(List<CourseDO> courseDOList, Map<Long, UserCourseSrsSettingDO> settingMap, Long userId) {
        Map<Long, CourseDO> courseMap = courseDOList.stream()
                .collect(Collectors.toMap(CourseDO::getId, course -> course));
        List<CourseMemoryBankDO> bankDOList = domainService.getBatchCourseCardStats(userId, courseMap.keySet());

        List<CourseMemoryBankDTO> bankDTOList = new ArrayList<>();
        for (CourseMemoryBankDO bankDO: bankDOList) {
            CourseMemoryBankDTO courseMemoryBankDTO = courseMemoryBankConverter.toCardStatsDTO(bankDO);
            courseMemoryBankDTO.setCourse(courseConverter.toBriefDTO(courseMap.get(bankDO.getCourseId())));
            courseMemoryBankDTO.setSetting(courseSrsSettingConverter.toDTO(settingMap.get(bankDO.getCourseId())));
            bankDTOList.add(courseMemoryBankDTO);
        }
        return bankDTOList;
    }


    // ========== Query 方法（读操作）==========

    /**
     * 获取记忆库课程列表
     */
    public List<CourseMemoryBankDTO> getMemoryBankCourses(Long userId, Integer state) {
        // 跨域验证：验证用户存在性
        userDataService.validateExists(userId);

        // 调用 DomainService 获取课程设置列表
        List<UserCourseSrsSettingDO> settings = domainService.getMemoryBankCourseSettings(userId, state);
        if (settings.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域查询：获取课程信息
        Set<Long> courseIds = settings.stream()
                .map(UserCourseSrsSettingDO::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(courseIds);

        // 构建设置映射
        Map<Long, UserCourseSrsSettingDO> settingMap = settings.stream()
                .collect(Collectors.toMap(UserCourseSrsSettingDO::getCourseId, s -> s));

        // 转换为DTO，过滤掉被屏蔽的课程（state != PUBLISHED）
        List<CourseDO> courses = settings.stream()
                .map(setting -> courseMap.get(setting.getCourseId()))
                .filter(Objects::nonNull)
                .filter(course -> ContentState.PUBLISHED.value().equals(course.getState()))
                .collect(Collectors.toList());

        return toCourseMemoryBankDTO(courses, settingMap, userId);
    }


    // ========== Command 方法（写操作）==========

    /**
     * 添加卡片组到记忆库
     */
    @Transactional
    public void addDeckToMemoryBank(Long userId, AddDeckToMemoryBankRequest request) {
        // 验证request参数
        checkNotNull(request);

        // 跨域验证：验证用户、卡片组、课程是否存在
        UserDO user = userDataService.validateAndGet(userId);
        MemoryCardDeckDO deck = deckDataService.validateAndGet(request.getDeckId());
        CourseDO course = courseDataService.validateAndGet(request.getCourseId());

        // 验证卡片组状态：只有已发布的卡片组才能加入复习序列
        if (!ContentState.PUBLISHED.value().equals(deck.getState())) {
            throw ErrorCode.OBJECT_STATE_INVALID.exception();
        }

        // 跨域查询：获取卡片列表
        List<MemoryCardDO> cards = cardDataService.getByDeckId(request.getDeckId());

        // 跨域查询：获取nodeId（deck.sourcePostId → post.nodeId）
        Long nodeId = null;
        if (deck.getPostId() != null) {
            PostDO post = postDataService.getById(deck.getPostId());
            if (post != null) {
                nodeId = post.getNodeId();
            }
        }
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");

        // 调用 DomainService 执行领域逻辑
        domainService.addDeckToMemoryBank(
            userId,
            request.getCourseId(),
            request.getDeckId(),
            cards,
            deck.getVersion(),
            nodeId
        );
    }

    /**
     * 更新课程复习策略
     */
    @Transactional
    public void updateCourseSetting(Long userId, Long courseId, UpdateCourseSettingRequest request) {
        // 验证参数
        checkNotNull(request);

        // 跨域验证：验证用户和课程是否存在
        userDataService.validateExists(userId);
        courseDataService.validateExists(courseId);

        // 调用 DomainService 执行领域逻辑
        domainService.updateCourseSetting(
            userId,
            courseId,
            request.getFrequencySetting(),
            request.getStatus() != null ? request.getStatus().byteValue() : null
        );

        // TODO: 触发异步任务重新计算复习时间
        // asyncRecalculationService.recalculateDueDates(userId, courseId);
    }

    /**
     * 移除课程中的卡片组
     */
    @Transactional
    public void removeDeckFromCourse(Long userId, Long courseId, Long deckId) {
        // 跨域验证：验证用户、课程、卡片组是否存在
        userDataService.validateExists(userId);
        courseDataService.validateExists(courseId);
        deckDataService.validateExists(deckId);

        // 跨域查询：获取卡片组中的所有卡片
        List<MemoryCardDO> cards = cardDataService.getByDeckId(deckId);
        if (cards == null || cards.isEmpty()) {
            return;
        }

        List<Long> cardIds = cards.stream()
                .map(MemoryCardDO::getId)
                .collect(Collectors.toList());

        // 调用 DomainService 执行领域逻辑
        domainService.removeDeckFromCourse(userId, courseId, cardIds);

        log.info("Removed deck: {} from course: {} for user: {}", deckId, courseId, userId);
    }
}