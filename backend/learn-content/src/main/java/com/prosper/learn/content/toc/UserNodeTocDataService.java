package com.prosper.learn.content.toc;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户节点目录数据服务
 * 负责用户节点目录数据的 CRUD 和缓存管理
 */
@Service
@RequiredArgsConstructor
public class UserNodeTocDataService {

    private final UserNodeTocMapper userNodeTocMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据用户ID和节点ID查询用户节点目录
     */
    @Cacheable(value = "userNodeToc", key = "#userId + '_' + #nodeId", unless = "#result == null")
    public UserNodeTocDO getByUserAndNode(long userId, long nodeId) {
        return userNodeTocMapper.getByUserAndNode(userId, nodeId);
    }

    /**
     * 批量查询用户节点目录
     *
     * @param userId 用户ID
     * @param nodeIds 节点ID列表
     * @return Map<nodeId, UserNodeTocDO>
     */
    public Map<Long, UserNodeTocDO> getByUserAndNodes(long userId, List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return Map.of();
        }
        List<UserNodeTocDO> list = userNodeTocMapper.getByUserAndNodes(userId, nodeIds);
        return list.stream().collect(Collectors.toMap(UserNodeTocDO::getNodeId, toc -> toc));
    }

    // ==================== 写入方法 ====================

    /**
     * 插入用户节点目录记录
     */
    public void insert(UserNodeTocDO userNodeTocDO) {
        userNodeTocMapper.insert(userNodeTocDO);
    }

    /**
     * 更新用户节点目录
     */
    @CacheEvict(value = "userNodeToc", key = "#userNodeTocDO.userId + '_' + #userNodeTocDO.nodeId")
    public void update(UserNodeTocDO userNodeTocDO) {
        userNodeTocMapper.update(userNodeTocDO);
    }
}
