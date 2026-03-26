package com.prosper.learn.memory.card;

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
 * 记忆卡片版本数据服务
 * 负责记忆卡片版本数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 只缓存单条查询 getById
 * - 列表/批量查询直接走数据库
 * - 写操作清除相关缓存
 */
@Service
@RequiredArgsConstructor
public class MemoryCardVersionDataService {

    private final MemoryCardVersionMapper memoryCardVersionMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询卡片版本
     */
    @Cacheable(value = "memoryCardVersions", key = "#id", unless = "#result == null")
    public MemoryCardVersionDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return memoryCardVersionMapper.get(id);
    }

    /**
     * 批量根据ID查询卡片版本
     */
    public List<MemoryCardVersionDO> getByIds(Collection<Long> ids) {
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
        return memoryCardVersionMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询卡片版本并转为Map
     */
    public Map<Long, MemoryCardVersionDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(MemoryCardVersionDO::getId, Function.identity()));
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取卡片版本
     */
    public MemoryCardVersionDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("卡片版本ID无效");
        }
        MemoryCardVersionDO version = getById(id);
        if (version == null) {
            throw StatusCode.MEMORY_CARD_VERSION_NOT_FOUND.exception();
        }
        return version;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入卡片版本
     */
    public int insert(MemoryCardVersionDO version) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        return memoryCardVersionMapper.insert(version);
    }

    /**
     * 批量插入卡片版本
     */
    public int batchInsert(List<MemoryCardVersionDO> versions) {
        if (versions == null || versions.isEmpty()) {
            return 0;
        }
        return memoryCardVersionMapper.batchInsert(versions);
    }

    /**
     * 更新版本激活状态
     */
    @CacheEvict(value = "memoryCardVersions", key = "#id")
    public boolean updateActiveStatus(long id, boolean isActive) {
        int result = memoryCardVersionMapper.updateActiveStatus(id, isActive);
        return result > 0;
    }
}
