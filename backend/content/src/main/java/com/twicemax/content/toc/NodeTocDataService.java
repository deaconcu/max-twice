package com.twicemax.content.toc;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 节点目录数据服务
 * 负责节点目录数据的 CRUD 和缓存管理
 */
@Service
@RequiredArgsConstructor
public class NodeTocDataService {

    private final NodeTocMapper nodeTocMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据hash获取目录
     */
    @Cacheable(value = "nodeTocs", key = "#hash", unless = "#result == null")
    public NodeTocDO get(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return null;
        }
        return nodeTocMapper.get(hash);
    }

    /**
     * 根据hash数组获取目录映射
     */
    public Map<String, NodeTocDO> getByHashes(String[] hashes) {
        if (hashes == null || hashes.length == 0) {
            return Map.of();
        }
        return nodeTocMapper.getByHashes(hashes);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入目录
     */
    public void insert(NodeTocDO nodeTocDO) {
        nodeTocMapper.insert(nodeTocDO);
    }

    /**
     * 增加引用计数
     */
    @CacheEvict(value = "nodeTocs", key = "#hash")
    public void incrRef(String hash, int n) {
        nodeTocMapper.incrRef(hash, n);
    }
}
