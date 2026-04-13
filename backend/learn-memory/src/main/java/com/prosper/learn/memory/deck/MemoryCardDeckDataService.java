package com.prosper.learn.memory.deck;

import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 记忆卡片组数据服务
 * 负责记忆卡片组数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 只缓存单条查询 getById
 * - 列表查询直接走数据库
 * - 写操作清除相关缓存
 */
@Service
@RequiredArgsConstructor
public class MemoryCardDeckDataService {

    private final MemoryCardDeckMapper memoryCardDeckMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询卡片组
     */
    @Cacheable(value = "memoryCardDecks", key = "#id", unless = "#result == null")
    public MemoryCardDeckDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return memoryCardDeckMapper.get(id);
    }

    /**
     * 批量根据ID查询卡片组
     */
    public List<MemoryCardDeckDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return memoryCardDeckMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询卡片组并转为Map
     */
    public Map<Long, MemoryCardDeckDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(MemoryCardDeckDO::getId, Function.identity()));
    }

    // ==================== 按帖子查询 ====================

    public List<MemoryCardDeckDO> getListByPostWithIdPaging(long postId, int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostWithIdPaging(postId, state, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByPostDynamic(long postId, int state, String sortBy, Double lastScore, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostDynamic(postId, state, sortBy, lastScore, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByPostForReview(long postId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostForReview(postId, state, limit);
    }

    // ==================== 按帖子和创建者查询 ====================

    public List<MemoryCardDeckDO> getListByPostAndCreator(long postId, long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreator(postId, creatorId, state, limit);
    }

    public List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPaging(long postId, long creatorId, byte state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorWithIdPaging(postId, creatorId, state, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByPostAndCreatorForReview(long postId, long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorForReview(postId, creatorId, state, limit);
    }

    public List<MemoryCardDeckDO> getListByPostAndCreatorAllStates(long postId, long creatorId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorAllStates(postId, creatorId, limit);
    }

    public List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPagingAllStates(long postId, long creatorId, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorWithIdPagingAllStates(postId, creatorId, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByPostAndCreatorDynamicAllStates(long postId, long creatorId, String sortBy, Double lastScore, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByPostAndCreatorDynamicAllStates(postId, creatorId, sortBy, lastScore, lastId, limit);
    }

    // ==================== 按创建者查询 ====================

    public List<MemoryCardDeckDO> getListByCreatorWithIdPaging(long creatorId, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByCreatorWithIdPaging(creatorId, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByCreatorWithIdPagingAndState(long creatorId, int state, Long lastId, int limit) {
        return memoryCardDeckMapper.getListByCreatorWithIdPagingAndState(creatorId, state, lastId, limit);
    }

    public List<MemoryCardDeckDO> getListByCreatorForReview(long creatorId, int state, int limit) {
        return memoryCardDeckMapper.getListByCreatorForReview(creatorId, state, limit);
    }

    // ==================== 按节点查询 ====================

    public List<MemoryCardDeckDO> getListByNode(long nodeId, int state, int limit) {
        return memoryCardDeckMapper.getListByNode(nodeId, state, limit);
    }

    public List<MemoryCardDeckDO> getListByNodeKeyset(long nodeId, double lastScore, long lastId, int state, int limit) {
        return memoryCardDeckMapper.getListByNodeKeyset(nodeId, lastScore, lastId, state, limit);
    }

    // ==================== 按状态查询 ====================

    public List<MemoryCardDeckDO> listByState(int state, Long lastId, int limit) {
        return memoryCardDeckMapper.listByState(state, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取卡片组
     */
    public MemoryCardDeckDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("卡片组ID无效");
        }
        MemoryCardDeckDO deck = getById(id);
        if (deck == null) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }
        return deck;
    }

    /**
     * 验证卡片组存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入卡片组
     */
    public int insert(MemoryCardDeckDO deck) {
        if (deck == null) {
            throw new IllegalArgumentException("Deck cannot be null");
        }
        return memoryCardDeckMapper.insert(deck);
    }

    /**
     * 更新卡片组
     */
    @CacheEvict(value = "memoryCardDecks", key = "#deck.id")
    public void update(MemoryCardDeckDO deck) {
        if (deck == null || deck.getId() == null) {
            throw new IllegalArgumentException("Deck or deck ID cannot be null");
        }
        memoryCardDeckMapper.update(deck);
    }

    /**
     * 软删除卡片组
     */
    @CacheEvict(value = "memoryCardDecks", key = "#deckId")
    public int softDelete(long deckId) {
        return memoryCardDeckMapper.softDelete(deckId);
    }

    /**
     * 增加卡片数量并设置状态和版本号
     */
    @CacheEvict(value = "memoryCardDecks", key = "#id")
    public boolean incrementCardCountAndSetStateAndVersion(long id, byte state) {
        int result = memoryCardDeckMapper.incrementCardCountAndSetStateAndVersion(id, state);
        return result > 0;
    }

    /**
     * 减少卡片数量并增加版本号
     */
    @CacheEvict(value = "memoryCardDecks", key = "#id")
    public boolean decrementCardCountAndIncrementVersion(long id) {
        int result = memoryCardDeckMapper.decrementCardCountAndIncrementVersion(id);
        return result > 0;
    }

    /**
     * 更新状态并增加版本号
     */
    @CacheEvict(value = "memoryCardDecks", key = "#id")
    public boolean updateStateAndIncrementVersion(long id, byte state) {
        int result = memoryCardDeckMapper.updateStateAndIncrementVersion(id, state);
        return result > 0;
    }
}
