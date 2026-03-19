package com.prosper.learn.memory.review;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户卡片SRS状态数据服务
 */
@Slf4j
@Service
public class UserCardSrsDataService extends AbstractDataService<UserCardSrsDO, UserCardSrsMapper, Long> {

    @Autowired
    private UserCardSrsMapper userCardSrsMapper;

    @Autowired
    private SystemProperties systemProperties;

    @Override
    protected UserCardSrsMapper mapper() {
        return userCardSrsMapper;
    }

    @Override
    protected String getCacheName() {
        return "user_card_srs";
    }

    @Override
    protected String getEntityName() {
        return "UserCardSrsState";
    }

    @Override
    protected Long getEntityId(UserCardSrsDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCardSrsDO getByIdFromMapper(UserCardSrsMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<UserCardSrsDO> getByIdsFromMapper(UserCardSrsMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, UserCardSrsDO> getMapByIdsFromMapper(UserCardSrsMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(30);
    }

    @Override
    protected int deleteByIdFromMapper(UserCardSrsMapper mapper, Long id) {
        return 0;
    }

    /**
     * 验证并获取SRS状态
     *
     * @param id SRS状态ID
     * @return SRS状态实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当SRS状态不存在时抛出 SRS_STATE_NOT_FOUND (2205)
     */
    @Override
    public UserCardSrsDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("SRS状态ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("SRS状态ID必须大于0");
        }

        UserCardSrsDO srs = getById(id);
        if (srs == null) {
            throw StatusCode.SRS_STATE_NOT_FOUND.exception();
        }

        return srs;
    }

    /**
     * 插入SRS状态
     */
    public int insert(UserCardSrsDO state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        try {
            return userCardSrsMapper.insert(state);
        } catch (Exception e) {
            log.error("Error inserting SRS state: userId={}, cardId={}", 
                     state.getUserId(), state.getCardId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新SRS状态并清除缓存
     */
    @CacheEvict(value = "user_card_srs", key = "#state.id")
    public void update(UserCardSrsDO state) {
        if (state == null || state.getId() == null) {
            throw new IllegalArgumentException("State or state ID cannot be null");
        }

        try {
            userCardSrsMapper.update(state);
            log.debug("Updated SRS state {}", state.getId());
        } catch (Exception e) {
            log.error("Error updating SRS state: {}", state.getId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 更新复习到期时间
//     */
//    //@CacheEvict(value = "user_card_srs", key = "#id")
//    public boolean updateReviewDueAt(long id, LocalDateTime reviewDueAt) {
//        try {
//            int result = userCardSrsMapper.updateReviewDueAt(id, reviewDueAt);
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error updating review due at: {}", id, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 复习后更新状态
//     */
//    public boolean updateAfterReview(long userId, long cardId, LocalDateTime reviewDueAt,
//                                     byte type, byte currentStep, int interval, Short lapseOldInterval,
//                                     BigDecimal easeFactor, int repetitions, int lapseCount) {
//        try {
//            // 先获取现有记录以获得ID用于清除缓存
//            UserCardSrsDO existingState = userCardSrsMapper.getByUserAndCard(userId, cardId);
//
//            int result = userCardSrsMapper.updateAfterReview(
//                userId, cardId, reviewDueAt, type, currentStep, interval, lapseOldInterval,
//                easeFactor, repetitions, lapseCount);
//
//            // 如果更新成功且存在记录，清除对应的缓存
//            if (result > 0 && existingState != null) {
//                evictCache(existingState.getId());
//            }
//
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error updating after review: userId={}, cardId={}", userId, cardId, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

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
     * 批量统计用户在多个 deck 中学习的卡片数量
     * @return Map<deckId, count>
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
     * 根据用户ID和节点ID获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUserAndNodeId(long userId, long nodeId) {
        return userCardSrsMapper.getByUserAndNodeId(userId, nodeId);
    }

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 获取到期的复习卡片
//     */
//    public List<UserCardSrsDO> getDueCardsForReview(long userId, LocalDateTime dueTime, int limit) {
//        return userCardSrsMapper.getDueCardsForReview(userId, dueTime, limit);
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 根据用户获取SRS状态列表
//     */
//    public List<UserCardSrsDO> getByUser(long userId, int limit) {
//        return userCardSrsMapper.getByUser(userId, limit);
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 根据用户和课程获取SRS状态列表
//     */
//    public List<UserCardSrsDO> getByUserAndCourse(long userId, long courseId) {
//        return userCardSrsMapper.getByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

    /**
     * 删除用户卡片的SRS状态
     */
    public boolean deleteByUserAndCard(long userId, long cardId) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCardSrsDO existingState = userCardSrsMapper.getByUserAndCard(userId, cardId);
            
            int result = userCardSrsMapper.deleteByUserAndCard(userId, cardId);
            
            // 如果删除成功且存在记录，清除对应的缓存
            if (result > 0 && existingState != null) {
                evictCache(existingState.getId());
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error deleting SRS state: userId={}, cardId={}", userId, cardId, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 统计用户的卡片数量
//     */
//    public int countByUser(long userId) {
//        return userCardSrsMapper.countByUser(userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 统计到期卡片数量
//     */
//    public int countDueCards(long userId, LocalDateTime dueTime) {
//        return userCardSrsMapper.countDueCards(userId, dueTime);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 统计用户指定课程的到期卡片数量
//     */
//    public long countDueCardsByUserAndCourse(long userId, long courseId) {
//        return userCardSrsMapper.countDueCardsByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 统计用户指定课程的新卡片数量（没有SRS状态的卡片）
//     */
//    public long countNewCardsByUserAndCourse(long userId, long courseId) {
//        return userCardSrsMapper.countNewCardsByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

// --注释掉检查 START (2025/12/10 11:29):
//    /**
//     * 统计用户指定课程的复习卡片数量（有SRS状态且不到期的卡片）
//     */
//    public long countReviewCardsByUserAndCourse(long userId, long courseId) {
//        return userCardSrsMapper.countReviewCardsByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:29)

// --注释掉检查 START (2025/12/10 11:28):
//    /**
//     * 统计用户指定课程的已学会卡片数量（重复次数 >= 3的卡片）
//     */
//    public long countLearnedCardsByUserAndCourse(long userId, long courseId) {
//        return userCardSrsMapper.countLearnedCardsByUserAndCourse(userId, courseId);
//    }
// --注释掉检查 STOP (2025/12/10 11:28)

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

    /**
     * 统计指定时间段内的复习次数
     */
    public long countReviewsInPeriod(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return userCardSrsMapper.countReviewsInPeriod(userId, startTime, endTime);
    }

    /**
     * 计算指定时间段内的平均分数
     */
    public double calculateAverageScore(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Double result = userCardSrsMapper.calculateAverageScore(userId, startTime, endTime);
        return result != null ? result : 0.0;
    }

    /**
     * 计算指定时间段内的学习时间（毫秒）
     */
    public long calculateTimeSpent(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Long result = userCardSrsMapper.calculateTimeSpent(userId, startTime, endTime);
        return result != null ? result : 0L;
    }

    /**
     * 批量插入SRS状态（使用INSERT IGNORE自动跳过已存在的）
     */
    public int batchInsertIgnoreSrsStates(List<UserCardSrsDO> states) {
        if (states == null || states.isEmpty()) {
            return 0;
        }
        try {
            int result = userCardSrsMapper.batchInsertIgnoreSrsStates(states);
            log.debug("Batch inserted {} SRS states", result);
            return result;
        } catch (Exception e) {
            log.error("Error batch inserting SRS states: stateCount={}", states.size(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

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
        srs.setType(UserCardSrsDO.TYPE_NEW);  // 新卡片
        srs.setCurrentStep((byte) 0);
        srs.setInterval(0);
        srs.setLapseOldInterval(null);

        srs.setReviewDueAt(now); // 立即可复习
        srs.setEaseFactor(new java.math.BigDecimal("2.5"));
        srs.setRepetitions(0);
        srs.setLapseCount(0);
        return srs;
    }

    /**
     * 批量删除用户指定卡片的SRS状态
     */
    public int batchDeleteByUserAndCards(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return 0;
        }
        try {
            int result = userCardSrsMapper.batchDeleteByUserAndCards(userId, cardIds);
            log.debug("Batch deleted {} SRS states for user: {}", result, userId);
            return result;
        } catch (Exception e) {
            log.error("Error batch deleting SRS states: userId={}, cardCount={}",
                     userId, cardIds.size(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    // ========== 新的复习逻辑：基于卡片计数的调度 ==========

    /**
     * 获取下一张待复习卡片（全部课程）
     *
     * 优先级：
     * 1. LEARNING/RELEARNING 且 reappear_at 已到
     * 2. REVIEW 或 NEW（根据 newFirst 参数决定顺序）
     * 3. LEARNING/RELEARNING 未到计数（兜底）
     *
     * @param userId 用户ID
     * @param reviewCardCount 用户当前的复习卡片计数
     * @param newFirst true=先新卡后复习，false=先复习后新卡
     * @return 下一张卡片的SRS状态，无卡片时返回null
     */
    public UserCardSrsDO getNextCard(long userId, long reviewCardCount, boolean newFirst) {
        // 1. 学习中到期的优先
        UserCardSrsDO card = userCardSrsMapper.getNextLearningCard(userId, reviewCardCount);
        if (card != null) {
            return card;
        }

        // 2. 根据 newFirst 决定顺序
        if (newFirst) {
            card = userCardSrsMapper.getNextNewCard(userId);
            if (card != null) {
                return card;
            }
            card = userCardSrsMapper.getNextReviewCard(userId);
            if (card != null) {
                return card;
            }
        } else {
            card = userCardSrsMapper.getNextReviewCard(userId);
            if (card != null) {
                return card;
            }
            card = userCardSrsMapper.getNextNewCard(userId);
            if (card != null) {
                return card;
            }
        }

        // 3. 兜底：学习中未到期的
        return userCardSrsMapper.getNextPendingLearningCard(userId, reviewCardCount);
    }

    /**
     * 获取下一张待复习卡片（指定课程）
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param reviewCardCount 用户当前的复习卡片计数
     * @param newFirst true=先新卡后复习，false=先复习后新卡
     * @return 下一张卡片的SRS状态，无卡片时返回null
     */
    public UserCardSrsDO getNextCardByCourse(long userId, long courseId, long reviewCardCount, boolean newFirst) {
        // 1. 学习中到期的优先
        UserCardSrsDO card = userCardSrsMapper.getNextLearningCardByCourse(userId, courseId, reviewCardCount);
        if (card != null) {
            return card;
        }

        // 2. 根据 newFirst 决定顺序
        if (newFirst) {
            card = userCardSrsMapper.getNextNewCardByCourse(userId, courseId);
            if (card != null) {
                return card;
            }
            card = userCardSrsMapper.getNextReviewCardByCourse(userId, courseId);
            if (card != null) {
                return card;
            }
        } else {
            card = userCardSrsMapper.getNextReviewCardByCourse(userId, courseId);
            if (card != null) {
                return card;
            }
            card = userCardSrsMapper.getNextNewCardByCourse(userId, courseId);
            if (card != null) {
                return card;
            }
        }

        // 3. 兜底：学习中未到期的
        return userCardSrsMapper.getNextPendingLearningCardByCourse(userId, courseId, reviewCardCount);
    }

    // ========== 单独查询方法（供 DomainService 直接调用）==========

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

    // ========== 复习统计 ==========

    /**
     * 统计用户的卡片总数
     *
     * @param userId 用户ID
     * @return 用户的卡片总数
     */
    public long countByUser(long userId) {
        return userCardSrsMapper.countByUser(userId);
    }

    /**
     * 统计今天复习过的卡片数（基于用户时区）
     *
     * @param userId 用户ID
     * @param userToday 用户时区的今天日期
     * @return 今天复习的卡片数
     */
    public long countTodayReviewed(long userId, LocalDate userToday) {
        LocalDateTime startOfDay = userToday.atStartOfDay();
        LocalDateTime endOfDay = userToday.plusDays(1).atStartOfDay();
        return userCardSrsMapper.countReviewedInRange(userId, startOfDay, endOfDay);
    }

    // ========== 按课程分组统计 ==========

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
     * 获取单个课程的卡片统计
     */
    public CourseMemoryBankDO getCardStatsForCourse(long userId, long courseId) {
        return userCardSrsMapper.getCardStatsForCourse(userId, courseId, LocalDateTime.now());
    }

    // ========== 优化统计查询（带 LIMIT）==========

    /**
     * 批量获取多个课程的卡片统计（优化版，每个课程独立 LIMIT）
     *
     * @param userId 用户ID
     * @param settings 课程设置列表（包含每个课程的 limit）
     * @return 课程统计列表
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

        // 1. 统计新卡片数（type=0），LIMIT = dailyNewLimit - todayNewCount（由调用方已处理）
        List<CourseMemoryBankDO> newList = userCardSrsMapper.countNewCardsWithLimit(userId, newParams);
        for (CourseMemoryBankDO d : newList) {
            CourseMemoryBankDO result = resultMap.get(d.getCourseId());
            if (result != null) {
                result.setNewCardCount(d.getNewCardCount());
            }
        }

        // 2. 统计 LEARNING/RELEARNING 卡片数（type IN (1,3)），不加 LIMIT
        List<CourseMemoryBankDO> learningList = userCardSrsMapper.countLearningCards(userId, courseIds);
        for (CourseMemoryBankDO d : learningList) {
            CourseMemoryBankDO result = resultMap.get(d.getCourseId());
            if (result != null) {
                result.setLearningCount(d.getLearningCount());
            }
        }

        // 3. 统计 REVIEW 且到期（type=2 AND due），LIMIT = dailyReviewLimit - todayReviewCount
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

    // ========== 移动节点到课程 ==========

    /**
     * 删除用户在指定节点下来自其他卡片组的卡片
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param excludeDeckId 要排除的卡片组ID（保留该卡片组的卡片）
     * @return 删除的卡片数量
     */
    public int deleteByUserAndNodeExcludeDeck(long userId, long nodeId, long excludeDeckId) {
        try {
            int result = userCardSrsMapper.deleteByUserAndNodeExcludeDeck(userId, nodeId, excludeDeckId);
            log.info("Deleted {} cards from other decks for user {} in node {} (excluding deck {})",
                    result, userId, nodeId, excludeDeckId);
            return result;
        } catch (Exception e) {
            log.error("Error deleting cards from other decks: userId={}, nodeId={}, excludeDeckId={}",
                    userId, nodeId, excludeDeckId, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 批量更新节点下所有卡片的课程归属
     */
    public int updateCourseIdByUserAndNode(long userId, long nodeId, long courseId) {
        try {
            int result = userCardSrsMapper.updateCourseIdByUserAndNode(userId, nodeId, courseId);
            log.info("Moved {} cards from node {} to course {} for user {}", result, nodeId, courseId, userId);
            return result;
        } catch (Exception e) {
            log.error("Error moving node to course: userId={}, nodeId={}, courseId={}", userId, nodeId, courseId, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

}