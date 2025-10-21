package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
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
public class NodeDataService extends AbstractDataService<NodeDO, NodeMapper, Long> {
    
    @Autowired
    private NodeMapper nodeMapper;
    
    @Override
    protected NodeMapper mapper() {
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

    @Override
    protected int deleteByIdFromMapper(NodeMapper mapper, Long id) {
        return 0;
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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 统计活跃节点数量
     */
    public Long countActiveNodes() {
        return nodeMapper.countActiveNodes();
    }

    public List<Long> selectIdsByUserIdAndPost(long afterId, long userId, int limit) {
        return nodeMapper.selectIdsByUserIdAndPost(afterId, userId, limit);
    }

    /**
     * 根据课程ID和节点名称查询节点
     */
    public NodeDO getByCourseAndName(long courseId, String name) {
        return nodeMapper.getByCourseAndName(courseId, name);
    }

    /**
     * 插入新节点
     */
    public void insert(NodeDO nodeDO) {
        nodeMapper.insert(nodeDO);
    }

    /**
     * 根据筛选条件获取节点列表（支持分页）
     */
    public List<NodeDO> getListByFilter(Long nodeId, Long courseId, Long creatorId, Long lastId) {
        if (lastId == null || lastId == 0) {
            lastId = Long.MAX_VALUE;
        }
        return nodeMapper.getListByFilterWithPagination(nodeId, courseId, creatorId, lastId, 20);
    }

    /**
     * 更新节点状态
     */
    @CacheEvict(value = "nodes", key = "#nodeId")
    public void updateState(Long nodeId, Enums.ContentState state) {
        if (nodeId == null || nodeId <= 0) {
            throw new IllegalArgumentException("Invalid node ID");
        }
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }

        try {
            nodeMapper.updateState(nodeId, state.value());
            log.debug("Updated node {} state to {}", nodeId, state);
        } catch (Exception e) {
            log.error("Error updating node state: {}", nodeId, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 审批通过节点
     */
    public void approve(long id) {
        nodeMapper.updateState(id, Enums.ContentState.PUBLISHED.value());
    }

    /**
     * 拒绝节点申请
     */
    @CacheEvict(value = "nodes", key = "#id")
    public void reject(long id) {
        nodeMapper.updateState(id, Enums.ContentState.REJECTED.value());
    }

    /**
     * 封禁节点
     */
    @CacheEvict(value = "nodes", key = "#id")
    public void ban(long id) {
        nodeMapper.updateState(id, Enums.ContentState.BANNED.value());
    }
}