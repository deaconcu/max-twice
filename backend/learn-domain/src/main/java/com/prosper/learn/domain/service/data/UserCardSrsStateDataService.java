package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.UserCardSrsStateDO;
import com.prosper.learn.persistence.mapper.UserCardSrsStateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
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
                                   int intervalDays, double easeFactor, int repetitions, int lapseCount) {
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

}