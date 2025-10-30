package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.UserCardSrsDO;
import com.prosper.learn.persistence.mapper.UserCardSrsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户卡片SRS状态数据服务
 */
@Slf4j
@Service
public class UserCardSrsDataService extends AbstractDataService<UserCardSrsDO, UserCardSrsMapper, Long> {

    @Autowired
    private UserCardSrsMapper userCardSrsMapper;

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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新SRS状态并清除缓存
     */
    //@CacheEvict(value = "user_card_srs", key = "#state.id")
    public void update(UserCardSrsDO state) {
        if (state == null || state.getId() == null) {
            throw new IllegalArgumentException("State or state ID cannot be null");
        }

        try {
            userCardSrsMapper.update(state);
            log.debug("Updated SRS state {}", state.getId());
        } catch (Exception e) {
            log.error("Error updating SRS state: {}", state.getId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新复习到期时间
     */
    //@CacheEvict(value = "user_card_srs", key = "#id")
    public boolean updateReviewDueAt(long id, LocalDateTime reviewDueAt) {
        try {
            int result = userCardSrsMapper.updateReviewDueAt(id, reviewDueAt);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating review due at: {}", id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 复习后更新状态
     */
    public boolean updateAfterReview(long userId, long cardId, LocalDateTime reviewDueAt,
                                     byte type, byte currentStep, int interval, Short lapseOldInterval,
                                     BigDecimal easeFactor, int repetitions, int lapseCount) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCardSrsDO existingState = userCardSrsMapper.getByUserAndCard(userId, cardId);

            int result = userCardSrsMapper.updateAfterReview(
                userId, cardId, reviewDueAt, type, currentStep, interval, lapseOldInterval,
                easeFactor, repetitions, lapseCount);

            // 如果更新成功且存在记录，清除对应的缓存
            if (result > 0 && existingState != null) {
                evictCache(existingState.getId());
            }

            return result > 0;
        } catch (Exception e) {
            log.error("Error updating after review: userId={}, cardId={}", userId, cardId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
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
     * 获取到期的复习卡片
     */
    public List<UserCardSrsDO> getDueCardsForReview(long userId, LocalDateTime dueTime, int limit) {
        return userCardSrsMapper.getDueCardsForReview(userId, dueTime, limit);
    }

    /**
     * 根据用户获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUser(long userId, int limit) {
        return userCardSrsMapper.getByUser(userId, limit);
    }

    /**
     * 根据用户和课程获取SRS状态列表
     */
    public List<UserCardSrsDO> getByUserAndCourse(long userId, long courseId) {
        return userCardSrsMapper.getByUserAndCourse(userId, courseId);
    }

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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 统计用户的卡片数量
     */
    public int countByUser(long userId) {
        return userCardSrsMapper.countByUser(userId);
    }

    /**
     * 统计到期卡片数量
     */
    public int countDueCards(long userId, LocalDateTime dueTime) {
        return userCardSrsMapper.countDueCards(userId, dueTime);
    }

    /**
     * 统计用户指定课程的到期卡片数量
     */
    public long countDueCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsMapper.countDueCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的新卡片数量（没有SRS状态的卡片）
     */
    public long countNewCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsMapper.countNewCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的复习卡片数量（有SRS状态且不到期的卡片）
     */
    public long countReviewCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsMapper.countReviewCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的已学会卡片数量（重复次数 >= 3的卡片）
     */
    public long countLearnedCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsMapper.countLearnedCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 获取用户的复习队列（所有课程）
     */
    public List<UserCardSrsDO> getReviewQueue(long userId, boolean dueOnly, int limit, Long lastId) {
        if (dueOnly) {
            return userCardSrsMapper.getDueCardsForReviewWithPaging(userId, LocalDateTime.now(), limit, lastId);
        } else {
            return userCardSrsMapper.getByUserWithPaging(userId, limit, lastId);
        }
    }

    /**
     * 获取用户指定课程的复习队列
     */
    public List<UserCardSrsDO> getReviewQueueByCourse(long userId, long courseId, boolean dueOnly, int limit, Long lastId) {
        if (dueOnly) {
            return userCardSrsMapper.getDueCardsByCourseForReviewWithPaging(userId, courseId, LocalDateTime.now(), limit, lastId);
        } else {
            return userCardSrsMapper.getByUserAndCourseWithPaging(userId, courseId, limit, lastId);
        }
    }

    /**
     * 统计指定时间段内的复习次数
     */
    public long countReviewsInPeriod(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return userCardSrsMapper.countReviewsInPeriod(userId, startTime, endTime);
    }

    /**
     * 计算用户的连续复习天数
     */
    public int calculateStreakDays(long userId) {
        return userCardSrsMapper.calculateStreakDays(userId);
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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 创建新的SRS状态对象
     */
    public UserCardSrsDO createNewSrsState(
            Long userId, Long cardId, Long deckId, Long nodeId, Integer deckVersion, Long cardVersionId) {
        LocalDateTime now = LocalDateTime.now();
        UserCardSrsDO srs = new UserCardSrsDO();
        srs.setUserId(userId);
        srs.setCardId(cardId);
        srs.setDeckId(deckId);
        srs.setNodeId(nodeId);
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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

}