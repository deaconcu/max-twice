package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.MemoryCardVersionDO;
import com.prosper.learn.persistence.mapper.MemoryCardVersionMapper;
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
 * 记忆卡片版本数据服务
 */
@Slf4j
@Service
public class MemoryCardVersionDataService extends AbstractDataService<MemoryCardVersionDO, MemoryCardVersionMapper, Long> {

    @Autowired
    private MemoryCardVersionMapper memoryCardVersionMapper;

    @Override
    protected MemoryCardVersionMapper mapper() {
        return memoryCardVersionMapper;
    }

    @Override
    protected String getCacheName() {
        return "memory_card_versions";
    }

    @Override
    protected String getEntityName() {
        return "MemoryCardVersion";
    }

    @Override
    protected Long getEntityId(MemoryCardVersionDO entity) {
        return entity.getId();
    }

    @Override
    protected MemoryCardVersionDO getByIdFromMapper(MemoryCardVersionMapper mapper, Long id) {
        return mapper.get(id);
    }

    @Override
    protected List<MemoryCardVersionDO> getByIdsFromMapper(MemoryCardVersionMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, MemoryCardVersionDO> getMapByIdsFromMapper(MemoryCardVersionMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofHours(1);
    }

    @Override
    protected int deleteByIdFromMapper(MemoryCardVersionMapper mapper, Long id) {
        return 0;
    }

    /**
     * 插入卡片版本
     */
    public int insert(MemoryCardVersionDO version) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }

        try {
            return memoryCardVersionMapper.insert(version);
        } catch (Exception e) {
            log.error("Error inserting card version: cardId={}", version.getCardId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 更新版本激活状态并清除缓存
     */
    @CacheEvict(value = "memory_card_versions", key = "#id")
    public boolean updateActiveStatus(long id, boolean isActive) {
        try {
            int result = memoryCardVersionMapper.updateActiveStatus(id, isActive);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating version active status: {}", id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 停用卡片的所有版本
     */
    public boolean deactivateAllVersions(long cardId) {
        try {
            // 先获取该卡片的所有版本以获得ID列表用于清除缓存
            List<MemoryCardVersionDO> versions = memoryCardVersionMapper.getVersionsByCard(cardId);
            
            int result = memoryCardVersionMapper.deactivateAllVersions(cardId);
            
            // 如果更新成功，清除相关的缓存记录
            if (result > 0 && !versions.isEmpty()) {
                for (MemoryCardVersionDO version : versions) {
                    evictCache(version.getId());
                }
                log.debug("Evicted cache for {} versions of card {}", versions.size(), cardId);
            }
            
            return result > 0;
        } catch (Exception e) {
            log.error("Error deactivating all versions for card: {}", cardId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 根据卡片获取所有版本
     */
    public List<MemoryCardVersionDO> getVersionsByCard(long cardId) {
        return memoryCardVersionMapper.getVersionsByCard(cardId);
    }

    /**
     * 根据卡片获取活跃版本
     */
    public MemoryCardVersionDO getActiveVersionByCard(long cardId) {
        return memoryCardVersionMapper.getActiveVersionByCard(cardId);
    }

    /**
     * 根据卡片和版本号获取版本
     */
    public MemoryCardVersionDO getVersionByCardAndVersion(long cardId, int version) {
        return memoryCardVersionMapper.getVersionByCardAndVersion(cardId, version);
    }

    /**
     * 根据内容哈希获取版本
     */
    public List<MemoryCardVersionDO> getByContentHash(String contentHash) {
        return memoryCardVersionMapper.getByContentHash(contentHash);
    }

    /**
     * 获取卡片的最大版本号
     */
    public Integer getMaxVersionByCard(long cardId) {
        return memoryCardVersionMapper.getMaxVersionByCard(cardId);
    }

    /**
     * 统计卡片的版本数量
     */
    public int countByCard(long cardId) {
        return memoryCardVersionMapper.countByCard(cardId);
    }

}