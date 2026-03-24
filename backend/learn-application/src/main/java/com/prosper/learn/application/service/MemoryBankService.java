package com.prosper.learn.application.service;

import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
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
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final UserStatsDomainService userStatsDomainService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final DailyLimitService dailyLimitService;
    private final SystemProperties systemProperties;

    // ========== DTO 转换方法 ==========

    /**
     * 转换为课程记忆库DTO
     */
    public CourseMemoryBankDTO toCourseMemoryBankDTO(CourseDO courseDO, UserCourseSrsSettingDO settingDO, Long userId) {
        CourseMemoryBankDO courseMemoryBankDO = domainService.getCourseCardStats(userId, courseDO.getId());
        CourseMemoryBankDTO bankDTO = courseMemoryBankConverter.toCardStatsDTO(courseMemoryBankDO);

        bankDTO.setCourse(courseConverter.toBriefDTO(courseDO));
        bankDTO.setSetting(courseSrsSettingConverter.toDTO(settingDO));

        // 添加今日计数（使用用户时区）
        LocalDate userToday = getUserToday(userId);
        bankDTO.setTodayNewCount(dailyLimitService.getTodayNewCount(userId, courseDO.getId(), userToday));
        bankDTO.setTodayReviewCount(dailyLimitService.getTodayReviewCount(userId, courseDO.getId(), userToday));

        return bankDTO;
    }

    public List<CourseMemoryBankDTO> toCourseMemoryBankDTO(List<CourseDO> courseDOList, Map<Long, UserCourseSrsSettingDO> settingMap, Long userId) {
        Map<Long, CourseDO> courseMap = courseDOList.stream()
                .collect(Collectors.toMap(CourseDO::getId, course -> course));
        List<CourseMemoryBankDO> bankDOList = domainService.getBatchCourseCardStats(userId, courseMap.keySet());

        // 获取用户时区的今天日期
        LocalDate userToday = getUserToday(userId);

        List<CourseMemoryBankDTO> bankDTOList = new ArrayList<>();
        for (CourseMemoryBankDO bankDO: bankDOList) {
            CourseMemoryBankDTO courseMemoryBankDTO = courseMemoryBankConverter.toCardStatsDTO(bankDO);
            courseMemoryBankDTO.setCourse(courseConverter.toBriefDTO(courseMap.get(bankDO.getCourseId())));
            courseMemoryBankDTO.setSetting(courseSrsSettingConverter.toDTO(settingMap.get(bankDO.getCourseId())));

            // 添加今日计数（使用用户时区）
            courseMemoryBankDTO.setTodayNewCount(dailyLimitService.getTodayNewCount(userId, bankDO.getCourseId(), userToday));
            courseMemoryBankDTO.setTodayReviewCount(dailyLimitService.getTodayReviewCount(userId, bankDO.getCourseId(), userToday));

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

        // 获取今日已复习数（今天 last_reviewed_at 被更新的卡片数）
        LocalDate userToday = getUserToday(userId);
        int todayCompleted = getTodayCompletedCount(userId, userToday);
        summary.setTodayCompleted(todayCompleted);

        // 获取连续复习天数
        int streakDays = userStatsDomainService.getReviewStreakDays(userId, userToday);
        summary.setStreakDays(streakDays);

        return summary;
    }

    /**
     * 获取今日已复习卡片数
     */
    private int getTodayCompletedCount(Long userId, LocalDate userToday) {
        try {
            return (int) userCardSrsDataService.countTodayReviewed(userId, userToday);
        } catch (Exception e) {
            log.error("获取今日已复习数失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 获取用户时区的"今天"日期
     */
    private LocalDate getUserToday(Long userId) {
        UserDO user = userDataService.getById(userId);
        String timezone = user != null ? user.getTimezone() : null;
        return TimeZoneUtil.getUserToday(timezone);
    }

    /**
     * 获取记忆库课程列表
     * - state=1（学习中）：返回课程列表 + 统计卡片数
     * - state=2（冻结）/state=3（隐藏）：只返回课程列表，不统计卡片
     */
    public List<CourseMemoryBankDTO> getMemoryBankCourses(Long userId, Integer state) {
        // 跨域验证：验证用户存在性
        userDataService.validateExists(userId);

        // 调用 DomainService 获取指定状态的课程设置列表
        List<UserCourseSrsSettingDO> settings = domainService.getMemoryBankCourseSettings(userId, state);
        if (settings.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域查询：获取课程信息
        Set<Long> courseIds = settings.stream()
                .map(UserCourseSrsSettingDO::getCourseId)
                .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(courseIds);

        // 过滤掉被屏蔽的课程（课程本身 state != PUBLISHED）
        List<UserCourseSrsSettingDO> validSettings = settings.stream()
                .filter(setting -> {
                    CourseDO course = courseMap.get(setting.getCourseId());
                    return course != null && ContentState.PUBLISHED.value().equals(course.getState());
                })
                .collect(Collectors.toList());

        if (validSettings.isEmpty()) {
            return new ArrayList<>();
        }

        // 只有学习中状态才统计卡片数
        boolean needStats = state != null && DeckCourseStudyState.STUDYING.value().equals(state.byteValue());

        Map<Long, CourseMemoryBankDO> statsMap = new HashMap<>();
        Map<Long, Integer> todayNewCounts = new HashMap<>();
        Map<Long, Integer> todayReviewCounts = new HashMap<>();
        if (needStats) {
            // 获取用户时区的今天日期
            LocalDate userToday = getUserToday(userId);
            // 预查各课程今日计数，传入 DataService 用于计算剩余额度
            for (UserCourseSrsSettingDO setting : validSettings) {
                Long courseId = setting.getCourseId();
                todayNewCounts.put(courseId, dailyLimitService.getTodayNewCount(userId, courseId, userToday));
                todayReviewCounts.put(courseId, dailyLimitService.getTodayReviewCount(userId, courseId, userToday));
            }
            List<CourseMemoryBankDO> statsList = domainService.getBatchCourseCardStatsOptimized(userId, validSettings, todayReviewCounts);
            statsMap = statsList.stream()
                    .collect(Collectors.toMap(CourseMemoryBankDO::getCourseId, s -> s));
        }

        // 构建返回结果
        List<CourseMemoryBankDTO> result = new ArrayList<>();
        for (UserCourseSrsSettingDO setting : validSettings) {
            Long courseId = setting.getCourseId();
            CourseDO course = courseMap.get(courseId);
            if (course == null) continue;

            CourseMemoryBankDTO dto = new CourseMemoryBankDTO();
            dto.setCourse(courseConverter.toBriefDTO(course));
            dto.setSetting(courseSrsSettingConverter.toDTO(setting));

            if (needStats && statsMap.containsKey(courseId)) {
                CourseMemoryBankDO stats = statsMap.get(courseId);
                int todayNewCount = todayNewCounts.getOrDefault(courseId, 0);
                int todayReviewCount = todayReviewCounts.getOrDefault(courseId, 0);
                // newCardCount 减去今日已学新卡数，dueCardCount 已由 DataService 按剩余额度限制
                dto.setNewCardCount(Math.max(0, (stats.getNewCardCount() != null ? stats.getNewCardCount() : 0) - todayNewCount));
                dto.setDueCardCount(stats.getDueCardCount() != null ? stats.getDueCardCount() : 0);
                dto.setLearningCount(stats.getLearningCount() != null ? stats.getLearningCount() : 0);
                dto.setReviewCardCount(stats.getReviewCardCount() != null ? stats.getReviewCardCount() : 0);
                dto.setTodayNewCount(todayNewCount);
                dto.setTodayReviewCount(todayReviewCount);
            } else {
                dto.setNewCardCount(0);
                dto.setDueCardCount(0);
                dto.setLearningCount(0);
                dto.setReviewCardCount(0);
                dto.setTodayNewCount(0);
                dto.setTodayReviewCount(0);
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * 获取单个课程的卡片统计（复习提交后实时刷新用）
     */
    public CourseMemoryBankDTO getSingleCourseStat(Long userId, Long courseId) {
        UserCourseSrsSettingDO setting = domainService.getMemoryBankCourseSettings(userId, null)
                .stream().filter(s -> s.getCourseId().equals(courseId)).findFirst().orElse(null);
        if (setting == null) return null;

        // 获取用户时区的今天日期
        LocalDate userToday = getUserToday(userId);
        int todayNewCount = dailyLimitService.getTodayNewCount(userId, courseId, userToday);
        int todayReviewCount = dailyLimitService.getTodayReviewCount(userId, courseId, userToday);

        Map<Long, Integer> todayReviewCounts = new HashMap<>();
        todayReviewCounts.put(courseId, todayReviewCount);

        List<CourseMemoryBankDO> statsList = domainService.getBatchCourseCardStatsOptimized(
                userId, List.of(setting), todayReviewCounts);
        if (statsList.isEmpty()) return null;

        CourseMemoryBankDO stats = statsList.get(0);
        CourseMemoryBankDTO dto = new CourseMemoryBankDTO();
        dto.setNewCardCount(Math.max(0, (stats.getNewCardCount() != null ? stats.getNewCardCount() : 0) - todayNewCount));
        dto.setDueCardCount(stats.getDueCardCount() != null ? stats.getDueCardCount() : 0);
        dto.setLearningCount(stats.getLearningCount() != null ? stats.getLearningCount() : 0);
        dto.setReviewCardCount(stats.getReviewCardCount() != null ? stats.getReviewCardCount() : 0);
        dto.setTodayNewCount(todayNewCount);
        dto.setTodayReviewCount(todayReviewCount);
        return dto;
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
     * @param request 更新请求，包含 frequencySetting、status、cardOrder、dailyNewLimit、dailyReviewLimit
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
            request.getCardOrder() != null ? request.getCardOrder().byteValue() : null,
            request.getDailyNewLimit(),
            request.getDailyReviewLimit()
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
        domainService.removeCardsFromCourse(userId, courseId, cardIds);

        log.info("Removed deck: {} from course: {} for user: {}", deckId, courseId, userId);
    }
}