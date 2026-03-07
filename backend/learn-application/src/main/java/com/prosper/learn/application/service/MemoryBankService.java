package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.CourseMemoryBankConverter;
import com.prosper.learn.application.converter.UserCourseSrsSettingConverter;
import com.prosper.learn.application.dto.request.AddDeckToMemoryBankRequest;
import com.prosper.learn.application.dto.request.UpdateCourseSettingRequest;
import com.prosper.learn.application.dto.response.CourseMemoryBankDTO;
import com.prosper.learn.application.dto.response.ReviewSummaryDTO;
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
import com.prosper.learn.shared.domain.exception.StatusCode;
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
     * 获取复习概览（包含课程列表和统计数据）
     *
     * @param userId 用户ID
     * @param state 状态过滤（可选）
     * @return 复习概览DTO
     */
    public ReviewSummaryDTO getReviewSummary(Long userId, Integer state) {
        ReviewSummaryDTO summary = new ReviewSummaryDTO();

        List<CourseMemoryBankDTO> courses = getMemoryBankCourses(userId, state);
        summary.setCourses(courses);

        if (!courses.isEmpty()) {
            // 计算今日待复习总数
            int todayTotal = courses.stream()
                    .mapToInt(bank -> bank.getDueCardCount() != null ? bank.getDueCardCount() : 0)
                    .sum();
            summary.setTodayTotal(todayTotal);
        }

        // TODO: 今日已复习数和连续天数需要从其他地方获取
        summary.setTodayCompleted(0);
        summary.setStreakDays(0);

        return summary;
    }

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
            throw StatusCode.OBJECT_STATE_INVALID.exception();
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
     *
     * <p>支持更新以下设置：
     * <ul>
     *   <li>frequencySetting - 复习频率：0=高频(0.75x间隔), 1=普通(1.0x间隔), 2=低频(1.5x间隔)</li>
     *   <li>status - 课程状态：1=学习中, 2=已暂停, 3=已归档</li>
     * </ul>
     *
     * <p><b>关于 frequencySetting 的应用时机：</b>
     * <p>修改 frequencySetting 后，<b>不需要</b>重新计算已有卡片的 reviewDueAt。
     * 新的频率设置会在每次复习提交时自动应用到间隔计算中，实现平滑过渡。
     *
     * <p><b>为什么不重新计算：</b>
     * <ol>
     *   <li>避免打乱用户已建立的复习节奏</li>
     *   <li>防止大量卡片突然变为到期，造成复习压力</li>
     *   <li>reviewDueAt 是基于历史复习时间计算的，改变它会破坏 SRS 算法的连续性</li>
     * </ol>
     *
     * <p><b>实际效果：</b>
     * <p>例如：将频率从"普通"改为"高频"后
     * <ul>
     *   <li>已到期的卡片：保持原定时间，复习后按高频计算下次间隔</li>
     *   <li>未到期的卡片：保持原定时间，到期复习后按高频计算下次间隔</li>
     *   <li>新添加的卡片：从一开始就按高频计算间隔</li>
     * </ul>
     *
     * <p>这种渐进式应用方式确保了复习体验的平滑性，避免突然的负担变化。
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param request 更新请求，包含 frequencySetting、status 和/或 cardOrder
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
            request.getStatus() != null ? request.getStatus().byteValue() : null,
            request.getCardOrder() != null ? request.getCardOrder().byteValue() : null
        );
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