package com.prosper.learn.business.service.data;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.MemoryCardDO;
import com.prosper.learn.persistence.mapper.MemoryCardMapper;
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
 * 记忆卡片数据服务
 */
@Slf4j
@Service
public class MemoryCardDataService extends AbstractDataService<MemoryCardDO, MemoryCardMapper, Long> {

    @Autowired
    private MemoryCardMapper memoryCardMapper;

    @Override
    protected MemoryCardMapper mapper() {
        return memoryCardMapper;
    }

    @Override
    protected String getCacheName() {
        return "memory_cards";
    }

    @Override
    protected String getEntityName() {
        return "MemoryCard";
    }

    @Override
    protected Long getEntityId(MemoryCardDO entity) {
        return entity.getId();
    }

    @Override
    protected MemoryCardDO getByIdFromMapper(MemoryCardMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<MemoryCardDO> getByIdsFromMapper(MemoryCardMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, MemoryCardDO> getMapByIdsFromMapper(MemoryCardMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(20);
    }

    @Override
    protected int deleteByIdFromMapper(MemoryCardMapper mapper, Long id) {
        return 0;
    }

    /**
     * 插入卡片
     */
    public int insert(MemoryCardDO card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }

        try {
            return memoryCardMapper.insert(card);
        } catch (Exception e) {
            log.error("Error inserting card: deckId={}", card.getDeckId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 批量插入卡片
     */
    public int batchInsert(List<MemoryCardDO> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        
        try {
            int result = memoryCardMapper.batchInsert(cards);
            log.info("Batch inserted {} memory cards", cards.size());
            return result;
        } catch (Exception e) {
            log.error("Error batch inserting memory cards: count={}", cards.size(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新卡片并清除缓存
     */
    @CacheEvict(value = "memory_cards", key = "#card.id")
    public void update(MemoryCardDO card) {
        if (card == null || card.getId() == null) {
            throw new IllegalArgumentException("Card or card ID cannot be null");
        }

        try {
            memoryCardMapper.update(card);
            log.debug("Updated card {}", card.getId());
        } catch (Exception e) {
            log.error("Error updating card: {}", card.getId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新卡片状态并清除缓存
     */
    @CacheEvict(value = "memory_cards", key = "#id")
    public boolean updateState(long id, int state) {
        try {
            int result = memoryCardMapper.updateState(id, state);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating card state: {}", id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 根据卡片组获取卡片列表
     */
    public List<MemoryCardDO> getListByDeck(long deckId, int state) {
        return memoryCardMapper.getListByDeck(deckId, state);
    }

    /**
     * 根据创建者获取卡片列表
     */
    public List<MemoryCardDO> getListByCreator(long creatorId, int state, int limit) {
        return memoryCardMapper.getListByCreator(creatorId, state, limit);
    }

    /**
     * 统计卡片组下的卡片数量
     */
    public int countByDeck(long deckId, int state) {
        return memoryCardMapper.countByDeck(deckId, state);
    }

    /**
     * 统计创建者的卡片数量
     */
    public int countByCreator(long creatorId, int state) {
        return memoryCardMapper.countByCreator(creatorId, state);
    }

    /**
     * 根据卡片组ID获取卡片列表（只获取已发布状态的卡片）
     */
    public List<MemoryCardDO> getByDeckId(Long deckId) {
        if (deckId == null) {
            return List.of();
        }
        return getListByDeck(deckId, Enums.ContentState.PUBLISHED.value());
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

    /**
     * 根据卡片组ID获取卡片ID列表（只获取正常状态的卡片）
     */
    public List<Long> getCardIdsByDeckId(Long deckId) {
        if (deckId == null) {
            return List.of();
        }
        return memoryCardMapper.getCardIdsByDeckId(deckId);
    }

    /**
     * 批量更新卡片的当前版本ID
     */
    public int batchUpdateCurrentVersionId(List<MemoryCardDO> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }

        try {
            int result = memoryCardMapper.batchUpdateCurrentVersionId(cards);
            log.info("Batch updated current version id for {} memory cards", cards.size());

            // 清除相关缓存
            for (MemoryCardDO card : cards) {
                evictCache(card.getId());
            }

            return result;
        } catch (Exception e) {
            log.error("Error batch updating current version id: count={}", cards.size(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 批量更新卡片
     */
    public int batchUpdate(List<MemoryCardDO> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }

        try {
            int result = memoryCardMapper.batchUpdate(cards);
            log.info("Batch updated {} memory cards", cards.size());

            // 清除相关缓存
            for (MemoryCardDO card : cards) {
                evictCache(card.getId());
            }

            return result;
        } catch (Exception e) {
            log.error("Error batch updating memory cards: count={}", cards.size(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

}