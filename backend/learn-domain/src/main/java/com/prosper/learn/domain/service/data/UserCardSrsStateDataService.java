package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.UserCardSrsStateDO;
import com.prosper.learn.persistence.mapper.UserCardSrsStateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
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
public class UserCardSrsStateDataService extends AbstractDataService<UserCardSrsStateDO, UserCardSrsStateMapper, Long> {

    @Autowired
    private UserCardSrsStateMapper userCardSrsStateMapper;

    @Override
    protected UserCardSrsStateMapper mapper() {
        return userCardSrsStateMapper;
    }

    @Override
    protected String getCacheName() {
        return "user_card_srs_states";
    }

    @Override
    protected String getEntityName() {
        return "UserCardSrsState";
    }

    @Override
    protected Long getEntityId(UserCardSrsStateDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCardSrsStateDO getByIdFromMapper(UserCardSrsStateMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<UserCardSrsStateDO> getByIdsFromMapper(UserCardSrsStateMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, UserCardSrsStateDO> getMapByIdsFromMapper(UserCardSrsStateMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(30);
    }

    @Override
    protected int deleteByIdFromMapper(UserCardSrsStateMapper mapper, Long id) {
        return 0;
    }

    /**
     * 插入SRS状态
     */
    public int insert(UserCardSrsStateDO state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        try {
            return userCardSrsStateMapper.insert(state);
        } catch (Exception e) {
            log.error("Error inserting SRS state: userId={}, cardId={}", 
                     state.getUserId(), state.getCardId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新SRS状态并清除缓存
     */
    @CacheEvict(value = "user_card_srs_states", key = "#state.id")
    public void update(UserCardSrsStateDO state) {
        if (state == null || state.getId() == null) {
            throw new IllegalArgumentException("State or state ID cannot be null");
        }

        try {
            userCardSrsStateMapper.update(state);
            log.debug("Updated SRS state {}", state.getId());
        } catch (Exception e) {
            log.error("Error updating SRS state: {}", state.getId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新复习到期时间
     */
    @CacheEvict(value = "user_card_srs_states", key = "#id")
    public boolean updateReviewDueAt(long id, LocalDateTime reviewDueAt) {
        try {
            int result = userCardSrsStateMapper.updateReviewDueAt(id, reviewDueAt);
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
                                     int intervalDays, BigDecimal easeFactor, int repetitions, int lapseCount) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCardSrsStateDO existingState = userCardSrsStateMapper.getByUserAndCard(userId, cardId);
            
            int result = userCardSrsStateMapper.updateAfterReview(
                userId, cardId, reviewDueAt, intervalDays, easeFactor, repetitions, lapseCount);
            
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
    public UserCardSrsStateDO getByUserAndCard(long userId, long cardId) {
        return userCardSrsStateMapper.getByUserAndCard(userId, cardId);
    }

    /**
     * 根据用户和多个卡片获取SRS状态列表
     */
    public List<UserCardSrsStateDO> getByUserAndCards(long userId, Collection<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userCardSrsStateMapper.getByUserAndCards(userId, cardIds);
    }

    /**
     * 获取到期的复习卡片
     */
    public List<UserCardSrsStateDO> getDueCardsForReview(long userId, LocalDateTime dueTime, int limit) {
        return userCardSrsStateMapper.getDueCardsForReview(userId, dueTime, limit);
    }

    /**
     * 根据用户获取SRS状态列表
     */
    public List<UserCardSrsStateDO> getByUser(long userId, int limit) {
        return userCardSrsStateMapper.getByUser(userId, limit);
    }

    /**
     * 根据用户和课程获取SRS状态列表
     */
    public List<UserCardSrsStateDO> getByUserAndCourse(long userId, long courseId) {
        return userCardSrsStateMapper.getByUserAndCourse(userId, courseId);
    }

    /**
     * 删除用户卡片的SRS状态
     */
    public boolean deleteByUserAndCard(long userId, long cardId) {
        try {
            // 先获取现有记录以获得ID用于清除缓存
            UserCardSrsStateDO existingState = userCardSrsStateMapper.getByUserAndCard(userId, cardId);
            
            int result = userCardSrsStateMapper.deleteByUserAndCard(userId, cardId);
            
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
        return userCardSrsStateMapper.countByUser(userId);
    }

    /**
     * 统计到期卡片数量
     */
    public int countDueCards(long userId, LocalDateTime dueTime) {
        return userCardSrsStateMapper.countDueCards(userId, dueTime);
    }

    /**
     * 统计用户指定课程的到期卡片数量
     */
    public long countDueCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsStateMapper.countDueCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的新卡片数量（没有SRS状态的卡片）
     */
    public long countNewCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsStateMapper.countNewCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的复习卡片数量（有SRS状态且不到期的卡片）
     */
    public long countReviewCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsStateMapper.countReviewCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 统计用户指定课程的已学会卡片数量（重复次数 >= 3的卡片）
     */
    public long countLearnedCardsByUserAndCourse(long userId, long courseId) {
        return userCardSrsStateMapper.countLearnedCardsByUserAndCourse(userId, courseId);
    }

    /**
     * 获取用户的复习队列（所有课程）
     */
    public List<UserCardSrsStateDO> getReviewQueue(long userId, boolean dueOnly, int limit) {
        if (dueOnly) {
            return userCardSrsStateMapper.getDueCardsForReview(userId, LocalDateTime.now(), limit);
        } else {
            return userCardSrsStateMapper.getByUser(userId, limit);
        }
    }

    /**
     * 获取用户指定课程的复习队列
     */
    public List<UserCardSrsStateDO> getReviewQueueByCourse(long userId, long courseId, boolean dueOnly, int limit) {
        if (dueOnly) {
            return userCardSrsStateMapper.getDueCardsByCourseForReview(userId, courseId, LocalDateTime.now(), limit);
        } else {
            return userCardSrsStateMapper.getByUserAndCourse(userId, courseId);
        }
    }

    /**
     * 统计指定时间段内的复习次数
     */
    public long countReviewsInPeriod(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return userCardSrsStateMapper.countReviewsInPeriod(userId, startTime, endTime);
    }

    /**
     * 计算用户的连续复习天数
     */
    public int calculateStreakDays(long userId) {
        return userCardSrsStateMapper.calculateStreakDays(userId);
    }

    /**
     * 计算指定时间段内的平均分数
     */
    public double calculateAverageScore(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Double result = userCardSrsStateMapper.calculateAverageScore(userId, startTime, endTime);
        return result != null ? result : 0.0;
    }

    /**
     * 计算指定时间段内的学习时间（毫秒）
     */
    public long calculateTimeSpent(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Long result = userCardSrsStateMapper.calculateTimeSpent(userId, startTime, endTime);
        return result != null ? result : 0L;
    }

}