package com.prosper.learn.memory.review;

import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户卡片SRS状态数据服务
 * 负责用户卡片SRS状态数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 只缓存单条查询 getById
 * - 列表查询直接走数据库
 * - 写操作清除相关缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCardSrsDataService {

    private final UserCardSrsMapper userCardSrsMapper;
    private final SystemProperties systemProperties;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询SRS状态
     */
    @Cacheable(value = "userCardSrs", key = "#id", unless = "#result == null")
    public UserCardSrsDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return userCardSrsMapper.get(id);
    }

    /**
     * 根据用户和卡片获取SRS状态
     */
    public UserCardSrsDO getByUserAndCard(long userId, long cardId) {
        return userCardSrsMapper.getByUserAndCard(userId, cardId);
    }

    /**
     * 根据用户和多个卡片获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUserAndCards(long userId, Collection<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userCardSrsMapper.getByUserAndCards(userId, cardIds);
    }

    /**
     * 根据用户和deck获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUserAndDeckId(long userId, long deckId) {
        return userCardSrsMapper.getByUserAndDeckId(userId, deckId);
    }

    /**
     * 根据用户ID和节点ID获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUserAndNodeId(long userId, long nodeId) {
        return userCardSrsMapper.getByUserAndNodeId(userId, nodeId);
    }

    /**
     * 获取用户的卡片列表（所有课程）
     */
    public List<UserCardSrsDO> getCardList(long userId, int limit, Long lastId) {
        return userCardSrsMapper.getByUserWithPaging(userId, limit, lastId);
    }

    /**
     * 获取用户指定课程的卡片列表
     */
    public List<UserCardSrsDO> getCardListByCourse(long userId, long courseId, int limit, Long lastId) {
        return userCardSrsMapper.getByUserAndCourseWithPaging(userId, courseId, limit, lastId);
    }

    // ==================== 统计方法 ====================

    /**
     * 统计用户的卡片总数
     */
    public long countByUser(long userId) {
        return userCardSrsMapper.countByUser(userId);
    }

    /**
     * 批量统计用户在多个 deck 中学习的卡片数量
     */
    public Map<Long, Integer> countByUserAndDeckIds(long userId, Collection<Long> deckIds) {
        if (deckIds == null || deckIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> results = userCardSrsMapper.countByUserAndDeckIds(userId, deckIds);
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : results) {
            Long deckId = ((Number) row.get("deck_id")).longValue();
            Integer count = ((Number) row.get("cnt")).intValue();
            countMap.put(deckId, count);
        }
        return countMap;
    }

    /**
     * 统计今天复习过的卡片数（基于用户时区）
     */
    public long countTodayReviewed(long userId, LocalDate userToday) {
        LocalDateTime startOfDay = userToday.atStartOfDay();
        LocalDateTime endOfDay = userToday.plusDays(1).atStartOfDay();
        return userCardSrsMapper.countReviewedInRange(userId, startOfDay, endOfDay);
    }

    // ==================== 课程统计方法 ====================

    /**
     * 获取单个课程的卡片统计
     */
    public CourseMemoryBankDO getCardStatsForCourse(long userId, long courseId) {
        return userCardSrsMapper.getCardStatsForCourse(userId, courseId, LocalDateTime.now());
    }

    /**
     * 批量获取多个课程的卡片统计
     */
    public List<CourseMemoryBankDO> getBatchCardStatsForCourses(long userId, Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userCardSrsMapper.getBatchCardStatsForCourses(userId, courseIds, LocalDateTime.now());
    }

    /**
     * 批量获取多个课程的卡片统计（优化版，每个课程独立 LIMIT）
     */
    public List<CourseMemoryBankDO> getBatchCardStatsOptimized(long userId, List<UserCourseSrsSettingDO> settings,
                                                                Map<Long, Integer> todayReviewCounts) {
        if (settings == null || settings.isEmpty()) {
            return new ArrayList<>();
        }

        int defaultNewLimit = systemProperties.getSrs().getDefaultDailyNewLimit();
        int defaultReviewLimit = systemProperties.getSrs().getDefaultDailyReviewLimit();

        List<Long> courseIds = settings.stream().map(UserCourseSrsSettingDO::getCourseId).collect(Collectors.toList());

        // 构建新卡查询参数
        List<CourseQueryParam> newParams = settings.stream()
                .map(s -> new CourseQueryParam(
                        s.getCourseId(),
                        s.getDailyNewLimit() != null ? s.getDailyNewLimit() : defaultNewLimit,
                        0
                ))
                .collect(Collectors.toList());

        // 初始化结果 Map
        Map<Long, CourseMemoryBankDO> resultMap = new HashMap<>();
        for (UserCourseSrsSettingDO setting : settings) {
            CourseMemoryBankDO d = new CourseMemoryBankDO();
            d.setCourseId(setting.getCourseId());
            d.setNewCardCount(0);
            d.setLearningCount(0);
            d.setReviewCardCount(0);
            d.setDueCardCount(0);
            resultMap.put(setting.getCourseId(), d);
        }

        // 1. 统计新卡片数
        List<CourseMemoryBankDO> newList = userCardSrsMapper.countNewCardsWithLimit(userId, newParams);
        for (CourseMemoryBankDO d : newList) {
            CourseMemoryBankDO result = resultMap.get(d.getCourseId());
            if (result != null) {
                result.setNewCardCount(d.getNewCardCount());
            }
        }

        // 2. 统计 LEARNING/RELEARNING 卡片数
        List<CourseMemoryBankDO> learningList = userCardSrsMapper.countLearningCards(userId, courseIds);
        for (CourseMemoryBankDO d : learningList) {
            CourseMemoryBankDO result = resultMap.get(d.getCourseId());
            if (result != null) {
                result.setLearningCount(d.getLearningCount());
            }
        }

        // 3. 统计 REVIEW 且到期
        LocalDateTime now = LocalDateTime.now();
        List<CourseQueryParam> reviewParams = settings.stream()
                .map(s -> {
                    int reviewLimit = s.getDailyReviewLimit() != null ? s.getDailyReviewLimit() : defaultReviewLimit;
                    int todayReviewCount = todayReviewCounts != null ? todayReviewCounts.getOrDefault(s.getCourseId(), 0) : 0;
                    int remaining = Math.max(0, reviewLimit - todayReviewCount);
                    return new CourseQueryParam(s.getCourseId(), 0, remaining);
                })
                .collect(Collectors.toList());

        List<CourseMemoryBankDO> reviewList = userCardSrsMapper.countReviewDueCardsWithLimit(userId, reviewParams, now);
        for (CourseMemoryBankDO d : reviewList) {
            CourseMemoryBankDO result = resultMap.get(d.getCourseId());
            if (result != null) {
                result.setReviewCardCount(d.getReviewCardCount());
                result.setDueCardCount(d.getReviewCardCount() != null ? d.getReviewCardCount() : 0);
            }
        }

        return new ArrayList<>(resultMap.values());
    }

    // ==================== 复习调度方法 ====================

    /**
     * 获取下一张学习中的卡片（指定课程）
     */
    public UserCardSrsDO getNextLearningCardByCourse(long userId, long courseId, long reviewCardCount) {
        return userCardSrsMapper.getNextLearningCardByCourse(userId, courseId, reviewCardCount);
    }

    /**
     * 获取下一张新卡片（指定课程）
     */
    public UserCardSrsDO getNextNewCardByCourse(long userId, long courseId) {
        return userCardSrsMapper.getNextNewCardByCourse(userId, courseId);
    }

    /**
     * 获取下一张复习卡片（指定课程）
     */
    public UserCardSrsDO getNextReviewCardByCourse(long userId, long courseId) {
        return userCardSrsMapper.getNextReviewCardByCourse(userId, courseId);
    }

    /**
     * 获取下一张待到期的学习中卡片（指定课程）
     */
    public UserCardSrsDO getNextPendingLearningCardByCourse(long userId, long courseId, long reviewCardCount) {
        return userCardSrsMapper.getNextPendingLearningCardByCourse(userId, courseId, reviewCardCount);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取SRS状态
     */
    public UserCardSrsDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("SRS状态ID无效");
        }
        UserCardSrsDO srs = getById(id);
        if (srs == null) {
            throw StatusCode.SRS_STATE_NOT_FOUND.exception();
        }
        return srs;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入SRS状态
     */
    public int insert(UserCardSrsDO state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        return userCardSrsMapper.insert(state);
    }

    /**
     * 更新SRS状态
     */
    @CacheEvict(value = "userCardSrs", key = "#state.id")
    public void update(UserCardSrsDO state) {
        if (state == null || state.getId() == null) {
            throw new IllegalArgumentException("State or state ID cannot be null");
        }
        userCardSrsMapper.update(state);
    }

    /**
     * 批量插入SRS状态（使用INSERT IGNORE自动跳过已存在的）
     */
    public int batchInsertIgnoreSrsStates(List<UserCardSrsDO> states) {
        if (states == null || states.isEmpty()) {
            return 0;
        }
        return userCardSrsMapper.batchInsertIgnoreSrsStates(states);
    }

    /**
     * 删除用户卡片的SRS状态
     */
    public boolean deleteByUserAndCard(long userId, long cardId) {
        UserCardSrsDO existingState = userCardSrsMapper.getByUserAndCard(userId, cardId);
        int result = userCardSrsMapper.deleteByUserAndCard(userId, cardId);
        if (result > 0 && existingState != null) {
            evictCacheById(existingState.getId());
        }
        return result > 0;
    }

    /**
     * 批量删除用户指定卡片的SRS状态
     */
    public int batchDeleteByUserAndCards(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return 0;
        }
        return userCardSrsMapper.batchDeleteByUserAndCards(userId, cardIds);
    }

    /**
     * 删除用户在指定节点下来自其他卡片组的卡片
     */
    public int deleteByUserAndNodeExcludeDeck(long userId, long nodeId, long excludeDeckId) {
        return userCardSrsMapper.deleteByUserAndNodeExcludeDeck(userId, nodeId, excludeDeckId);
    }

    /**
     * 批量更新节点下所有卡片的课程归属
     */
    public int updateCourseIdByUserAndNode(long userId, long nodeId, long courseId) {
        return userCardSrsMapper.updateCourseIdByUserAndNode(userId, nodeId, courseId);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新的SRS状态对象
     */
    public UserCardSrsDO createNewSrsState(
            Long userId, Long cardId, Long deckId, Long nodeId, Long courseId, Integer deckVersion, Long cardVersionId) {
        LocalDateTime now = LocalDateTime.now();
        UserCardSrsDO srs = new UserCardSrsDO();
        srs.setUserId(userId);
        srs.setCardId(cardId);
        srs.setDeckId(deckId);
        srs.setNodeId(nodeId);
        srs.setCourseId(courseId);
        srs.setDeckVersion(deckVersion);
        srs.setCardVersionId(cardVersionId);

        // Anki 算法初始化
        srs.setType(UserCardSrsDO.TYPE_NEW);
        srs.setCurrentStep((byte) 0);
        srs.setInterval(0);
        srs.setLapseOldInterval(null);

        srs.setReviewDueAt(now);
        srs.setEaseFactor(new java.math.BigDecimal("2.5"));
        srs.setRepetitions(0);
        srs.setLapseCount(0);
        return srs;
    }

    // ==================== 缓存辅助方法 ====================

    @CacheEvict(value = "userCardSrs", key = "#id")
    public void evictCacheById(Long id) {
        // 仅用于清除缓存
    }
}
