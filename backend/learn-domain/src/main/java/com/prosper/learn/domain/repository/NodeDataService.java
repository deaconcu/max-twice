package com.prosper.learn.domain.repository;

import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.mapper.NodeMapper;
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
 * 节点数据服务，提供缓存功能
 * 专注于数据访问和缓存管理，避免循环依赖
 */
@Slf4j
@Service
public class NodeDataService extends AbstractDataService<NodeDO, NodeMapper> {
    
    @Autowired
    private NodeMapper nodeMapper;
    
    @Override
    protected NodeMapper getMapper() {
        return nodeMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "nodes";
    }
    
    @Override
    protected String getEntityName() {
        return "Node";
    }
    
    @Override
    protected Long getEntityId(NodeDO entity) {
        return entity.getId();
    }
    
    @Override
    protected NodeDO getByIdFromMapper(NodeMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<NodeDO> getByIdsFromMapper(NodeMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }
    
    @Override
    protected Map<Long, NodeDO> getMapByIdsFromMapper(NodeMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }
    
    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(15);
    }
    
    /**
     * 更新节点并清除缓存
     */
    @CacheEvict(value = "nodes", key = "#node.id")
    public void update(NodeDO node) {
        if (node == null || node.getId() == null) {
            throw new IllegalArgumentException("Node or node ID cannot be null");
        }
        
        try {
            nodeMapper.update(node);
            log.debug("Updated node {}", node.getId());
        } catch (Exception e) {
            log.error("Error updating node: {}", node.getId(), e);
            throw new RuntimeException("Failed to update node: " + node.getId(), e);
        }
    }
}