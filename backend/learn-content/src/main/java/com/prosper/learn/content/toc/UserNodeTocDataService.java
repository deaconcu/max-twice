package com.prosper.learn.content.toc;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户节点目录数据服务
 */
@Service
public class UserNodeTocDataService extends AbstractDataService<UserNodeTocDO, UserNodeTocMapper, Long> {

    @Autowired
    private UserNodeTocMapper userNodeTocMapper;

    @Override
    protected UserNodeTocMapper mapper() {
        return userNodeTocMapper;
    }

    @Override
    protected String getCacheName() {
        return "userNodeTocs";
    }

    @Override
    protected String getEntityName() {
        return "UserNodeToc";
    }

    @Override
    protected Long getEntityId(UserNodeTocDO entity) {
        return entity.getId();
    }

    @Override
    protected UserNodeTocDO getByIdFromMapper(UserNodeTocMapper mapper, Long id) {
        return userNodeTocMapper.get(id);
    }

    @Override
    protected List<UserNodeTocDO> getByIdsFromMapper(UserNodeTocMapper mapper, Collection<Long> ids) {
        return List.of(); // UserNodeTocMapper没有批量查询方法
    }

    @Override
    protected Map<Long, UserNodeTocDO> getMapByIdsFromMapper(UserNodeTocMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserNodeTocMapper没有批量查询方法
    }

    @Override
    protected int deleteByIdFromMapper(UserNodeTocMapper mapper, Long id) {
        return 0;
    }

    /**
     * 验证并获取用户节点目录
     *
     * @param id 用户节点目录ID
     * @return 用户节点目录实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当用户节点目录不存在时抛出 TOC_USER_TOC_NOT_FOUND (1801)
     */
    @Override
    public UserNodeTocDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户节点目录ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户节点目录ID必须大于0");
        }

        UserNodeTocDO toc = getById(id);
        if (toc == null) {
            throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
        }

        return toc;
    }

    /**
     * 根据用户ID和节点ID查询用户节点目录
     */
    @Cacheable(value = "userNodeTocByUserAndNode", key = "#userId + '_' + #nodeId")
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
        return list.stream().collect(java.util.stream.Collectors.toMap(
            UserNodeTocDO::getNodeId,
            toc -> toc
        ));
    }

    /**
     * 更新用户节点目录
     */
    @CacheEvict(value = "userNodeTocByUserAndNode", key = "#userNodeTocDO.userId + '_' + #userNodeTocDO.nodeId")
    public void update(UserNodeTocDO userNodeTocDO) {
        userNodeTocMapper.update(userNodeTocDO);
    }

    /**
     * 删除用户节点目录记录
     */
    @CacheEvict(value = "userNodeTocByUserAndNode", key = "#userNodeTocDO.userId + '_' + #userNodeTocDO.nodeId")
    public void delete(UserNodeTocDO userNodeTocDO) {
        userNodeTocMapper.delete(userNodeTocDO.getId());
    }

    /**
     * 插入用户节点目录记录
     */
    public void insert(UserNodeTocDO userNodeTocDO) {
        userNodeTocMapper.insert(userNodeTocDO);
    }
}