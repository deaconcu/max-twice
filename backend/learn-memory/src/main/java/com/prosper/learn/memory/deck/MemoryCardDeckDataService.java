package com.prosper.learn.memory.deck;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆卡片组数据服务
 */
@Slf4j
@Service
public class MemoryCardDeckDataService extends AbstractDataService<MemoryCardDeckDO, MemoryCardDeckMapper, Long> {

    @Autowired
    private MemoryCardDeckMapper memoryCardDeckMapper;

    @Override
    protected MemoryCardDeckMapper mapper() {
        return memoryCardDeckMapper;
    }

    @Override
    protected String getCacheName() {
        return "memory_card_decks";
    }

    @Override
    protected String getEntityName() {
        return "MemoryCardDeck";
    }

    @Override
    protected Long getEntityId(MemoryCardDeckDO entity) {
        return entity.getId();
    }

    @Override
    protected MemoryCardDeckDO getByIdFromMapper(MemoryCardDeckMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<MemoryCardDeckDO> getByIdsFromMapper(MemoryCardDeckMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, MemoryCardDeckDO> getMapByIdsFromMapper(MemoryCardDeckMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(15);
    }

    @Override
    protected int deleteByIdFromMapper(MemoryCardDeckMapper mapper, Long id) {
        return 0;
    }

    /**
     * 插入卡片组
     */
    public int insert(MemoryCardDeckDO deck) {
        if (deck == null) {
            throw new IllegalArgumentException("Deck cannot be null");
        }
        
        try {
            return memoryCardDeckMapper.insert(deck);
        } catch (Exception e) {
            log.error("Error inserting deck: {}", deck.getTitle(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新卡片组并清除缓存
     */
    @CacheEvict(value = "memory_card_decks", key = "#deck.id")
    public void update(MemoryCardDeckDO deck) {
        if (deck == null || deck.getId() == null) {
            throw new IllegalArgumentException("Deck or deck ID cannot be null");
        }

        try {
            memoryCardDeckMapper.update(deck);
            log.debug("Updated deck {}", deck.getId());
        } catch (Exception e) {
            log.error("Error updating deck: {}", deck.getId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新审核状态并清除缓存
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean updateAuditStatus(long id, int state, long auditorId) {
        try {
            int result = memoryCardDeckMapper.updateAuditStatus(id, state, auditorId);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating deck audit status: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新分数并清除缓存
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean updateScore(long id, int upvoteCount, double score) {
        try {
            int result = memoryCardDeckMapper.updateScore(id, score);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating deck score: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新卡片数量并清除缓存
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean updateCardCount(long id, int cardCount) {
        try {
            int result = memoryCardDeckMapper.updateCardCount(id, cardCount);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating deck card count: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新状态并清除缓存
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean updateState(long id, byte state) {
        try {
            int result = memoryCardDeckMapper.updateState(id, state);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating deck state: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 原子操作：增加卡片数量并设置状态
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean incrementCardCountAndSetState(long id, byte state) {
        try {
            int result = memoryCardDeckMapper.incrementCardCountAndSetState(id, state);
            return result > 0;
        } catch (Exception e) {
            log.error("Error incrementing card count and setting state: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 原子操作：增加卡片数量、设置状态并增加版本号
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean incrementCardCountAndSetStateAndVersion(long id, byte state) {
        try {
            int result = memoryCardDeckMapper.incrementCardCountAndSetStateAndVersion(id, state);
            return result > 0;
        } catch (Exception e) {
            log.error("Error incrementing card count, setting state and incrementing version: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 原子操作：减少卡片数量（保证不会小于0）
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean decrementCardCount(long id) {
        try {
            int result = memoryCardDeckMapper.decrementCardCount(id);
            return result > 0;
        } catch (Exception e) {
            log.error("Error decrementing card count: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 原子操作：减少卡片数量并增加版本号
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean decrementCardCountAndIncrementVersion(long id) {
        try {
            int result = memoryCardDeckMapper.decrementCardCountAndIncrementVersion(id);
            return result > 0;
        } catch (Exception e) {
            log.error("Error decrementing card count and incrementing version: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 原子操作：更新状态并增加版本号
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean updateStateAndIncrementVersion(long id, byte state) {
        try {
            int result = memoryCardDeckMapper.updateStateAndIncrementVersion(id, state);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating state and incrementing version: {}", id, e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 根据帖子获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByPost(long postId, int state, int limit) {
        return memoryCardDeckMapper.getListByPost(postId, state, limit);
    }

    /**
     * 根据帖子获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByPostWithIdPaging(long postId, int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostWithIdPaging(postId, state, lastId, limit);
    }

    /**
     * 根据帖子获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByPostKeyset(long postId, double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostKeyset(postId, lastScore, lastId, state, limit);
    }

    /**
     * 根据创建者获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByCreator(long creatorId, int limit) {
        return memoryCardDeckMapper.getListByCreator(creatorId, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByCreatorWithIdPaging(long creatorId, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByCreatorWithIdPaging(creatorId, lastId, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - ID分页（带状态过滤）
     */
    public List<MemoryCardDeckDO> getListByCreatorWithIdPagingAndState(long creatorId, int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByCreatorWithIdPagingAndState(creatorId, state, lastId, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByCreatorKeyset(long creatorId, double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByCreatorKeyset(creatorId, lastScore, lastId, state, limit);
    }

    /**
     * 根据状态获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByState(int state, int limit) {
        return memoryCardDeckMapper.getListByState(state, limit);
    }

    /**
     * 根据状态获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByStateKeyset(double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByStateKeyset(lastScore, lastId, state, limit);
    }

    /**
     * 根据状态获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByStateWithIdPaging(int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByStateWithIdPaging(state, lastId, limit);
    }

    /**
     * 根据帖子获取卡片组列表 - 审核专用
     */
    public List<MemoryCardDeckDO> getListByPostForReview(long postId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostForReview(postId, state, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - 审核专用
     */
    public List<MemoryCardDeckDO> getListByCreatorForReview(long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByCreatorForReview(creatorId, state, limit);
    }

    /**
     * 根据状态获取卡片组列表 - 审核专用
     */
    public List<MemoryCardDeckDO> getListByStateForReview(int state, int limit) {
        return memoryCardDeckMapper.getListByStateForReview(state, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - 审核专用
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorForReview(long postId, long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorForReview(postId, creatorId, state, limit);
    }

    /**
     * 统计帖子下的卡片组数量
     */
    public int countByPost(long postId, int state) {
        return memoryCardDeckMapper.countByPost(postId, state);
    }

    /**
     * 统计创建者的卡片组数量
     */
    public int countByCreator(long creatorId, int state) {
        return memoryCardDeckMapper.countByCreator(creatorId, state);
    }

    /**
     * 根据帖子和创建者获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByPostAndCreator(long postId, long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreator(postId, creatorId, state, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPaging(long postId, long creatorId, int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorWithIdPaging(postId, creatorId, state, lastId, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorKeyset(long postId, long creatorId, double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorKeyset(postId, creatorId, lastScore, lastId, state, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - 所有状态
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorAllStates(long postId, long creatorId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorAllStates(postId, creatorId, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - Keyset分页，所有状态
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorKeysetAllStates(long postId, long creatorId, double lastScore, long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorKeysetAllStates(postId, creatorId, lastScore, lastId, limit);
    }

    /**
     * 根据节点ID获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByNode(long nodeId, int state, int limit) {
        return memoryCardDeckMapper.getListByNode(nodeId, state, limit);
    }

    /**
     * 根据节点ID获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByNodeKeyset(long nodeId, double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByNodeKeyset(nodeId, lastScore, lastId, state, limit);
    }

    /**
     * 原子操作：减少点赞数（保证不会小于0）
     */
    @CacheEvict(value = "memory_card_decks", key = "#id")
    public boolean decrementUpvoteCount(long id) {
        return false;
    }

    public int softDelete(Long deckId) {
        return memoryCardDeckMapper.softDelete(deckId);
    }
}