package com.twicemax.content.node;

import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 节点数据服务
 * 负责节点数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeDataService {

    private final NodeMapper nodeMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询节点
     */
    @Cacheable(value = "nodes", key = "#id", unless = "#result == null")
    public NodeDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return nodeMapper.getById(id);
    }

    /**
     * 批量根据ID查询节点
     */
    public List<NodeDO> getByIds(Collection<Long> ids) {
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
        return nodeMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询节点并转为Map
     */
    public Map<Long, NodeDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(NodeDO::getId, Function.identity()));
    }

    /**
     * 统计活跃节点数量
     */
    public Long countActiveNodes() {
        return nodeMapper.countActiveNodes();
    }

    /**
     * 查询用户有帖子的节点ID列表
     */
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
     * 按状态获取节点列表（支持正序/倒序分页）
     */
    public List<NodeDO> listByState(Byte state, Long lastId, int limit, boolean orderAsc) {
        return nodeMapper.listByState(state, lastId, limit, orderAsc);
    }

    /**
     * 高级筛选节点列表
     */
    public List<NodeDO> listByFilter(Long nodeId, Long courseId, Long creatorId, Long lastId, int limit) {
        return nodeMapper.listByFilter(nodeId, courseId, creatorId, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证节点ID并获取节点
     */
    public NodeDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("节点ID无效");
        }
        NodeDO node = getById(id);
        if (node == null) {
            throw StatusCode.NODE_NOT_FOUND.exception();
        }
        return node;
    }

    /**
     * 验证节点存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入节点
     */
    public void insert(NodeDO nodeDO) {
        nodeMapper.insert(nodeDO);
    }

    /**
     * 更新节点
     */
    @CacheEvict(value = "nodes", key = "#node.id")
    public void update(NodeDO node) {
        if (node == null || node.getId() == null) {
            throw new IllegalArgumentException("Node or node ID cannot be null");
        }
        nodeMapper.update(node);
    }

    /**
     * 审批通过节点
     */
    @CacheEvict(value = "nodes", key = "#id")
    public void approve(long id) {
        nodeMapper.updateStateAndReason(id, Enums.ContentState.PUBLISHED.value(), "");
    }

    /**
     * 拒绝节点申请
     */
    @CacheEvict(value = "nodes", key = "#id")
    public void reject(long id, String reason) {
        nodeMapper.updateStateAndReason(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁节点
     */
    @CacheEvict(value = "nodes", key = "#id")
    public void ban(long id, String reason) {
        nodeMapper.updateStateAndReason(id, Enums.ContentState.BANNED.value(), reason);
    }
}
