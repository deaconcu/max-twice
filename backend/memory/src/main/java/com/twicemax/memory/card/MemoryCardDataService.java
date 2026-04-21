package com.twicemax.memory.card;

import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 记忆卡片数据服务
 * 负责记忆卡片数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 只缓存单条查询 getById
 * - 列表查询直接走数据库
 * - 写操作清除相关缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDataService {

    private final MemoryCardMapper memoryCardMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询卡片
     */
    @Cacheable(value = "memoryCards", key = "#id", unless = "#result == null")
    public MemoryCardDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return memoryCardMapper.get(id);
    }

    /**
     * 批量根据ID查询卡片
     */
    public List<MemoryCardDO> getByIds(Collection<Long> ids) {
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
        return memoryCardMapper.getByIds(validIds);
    }

    /**
     * 根据卡片组ID获取卡片列表（只获取已发布状态的卡片）
     */
    public List<MemoryCardDO> getByDeckId(Long deckId) {
        if (deckId == null) {
            return List.of();
        }
        return memoryCardMapper.getListByDeck(deckId, Enums.ContentState.PUBLISHED.value());
    }

    /**
     * 批量根据卡片组ID列表获取卡片（只获取已发布状态的卡片）
     */
    public List<MemoryCardDO> getByDeckIds(List<Long> deckIds) {
        if (deckIds == null || deckIds.isEmpty()) {
            return List.of();
        }
        return memoryCardMapper.getByDeckIds(deckIds, Enums.ContentState.PUBLISHED.value());
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取卡片
     */
    public MemoryCardDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("卡片ID无效");
        }
        MemoryCardDO card = getById(id);
        if (card == null) {
            throw StatusCode.MEMORY_CARD_NOT_FOUND.exception();
        }
        return card;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入卡片
     */
    public int insert(MemoryCardDO card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }
        return memoryCardMapper.insert(card);
    }

    /**
     * 批量插入卡片
     */
    public int batchInsert(List<MemoryCardDO> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        return memoryCardMapper.batchInsert(cards);
    }

    /**
     * 更新卡片
     */
    @CacheEvict(value = "memoryCards", key = "#card.id")
    public void update(MemoryCardDO card) {
        if (card == null || card.getId() == null) {
            throw new IllegalArgumentException("Card or card ID cannot be null");
        }
        memoryCardMapper.update(card);
    }

    /**
     * 批量更新卡片的当前版本ID
     */
    public int batchUpdateCurrentVersionId(List<MemoryCardDO> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        int result = memoryCardMapper.batchUpdateCurrentVersionId(cards);
        // 清除相关缓存
        for (MemoryCardDO card : cards) {
            evictCacheById(card.getId());
        }
        return result;
    }

    /**
     * 软删除卡片
     */
    @CacheEvict(value = "memoryCards", key = "#card.id")
    public int softDelete(MemoryCardDO card) {
        if (card == null || card.getId() == null) {
            throw new IllegalArgumentException("Card or card ID cannot be null");
        }
        return memoryCardMapper.softDelete(card);
    }

    /**
     * 批量软删除卡片
     */
    public int batchSoftDelete(List<Long> cardIds, LocalDateTime now) {
        if (cardIds == null || cardIds.isEmpty()) {
            return 0;
        }
        int result = memoryCardMapper.batchSoftDelete(cardIds, now);
        // 清除相关缓存
        for (Long cardId : cardIds) {
            evictCacheById(cardId);
        }
        return result;
    }

    // ==================== 缓存辅助方法 ====================

    @CacheEvict(value = "memoryCards", key = "#id")
    public void evictCacheById(Long id) {
        // 仅用于清除缓存
    }
}
