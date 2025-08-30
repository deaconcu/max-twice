package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户路线图数据服务
 */
@Service
public class UserRoadmapDataService extends AbstractDataService<UserRoadmapDO, UserRoadmapMapper, Long> {

    @Autowired
    private UserRoadmapMapper userRoadmapMapper;

    @Override
    protected UserRoadmapMapper mapper() {
        return userRoadmapMapper;
    }

    @Override
    protected String getCacheName() {
        return "userRoadmaps";
    }

    @Override
    protected String getEntityName() {
        return "UserRoadmap";
    }

    @Override
    protected Long getEntityId(UserRoadmapDO entity) {
        return entity.getId();
    }

    @Override
    protected UserRoadmapDO getByIdFromMapper(UserRoadmapMapper mapper, Long id) {
        return null; // UserRoadmapMapper没有getById方法
    }

    @Override
    protected List<UserRoadmapDO> getByIdsFromMapper(UserRoadmapMapper mapper, Collection<Long> ids) {
        return List.of(); // UserRoadmapMapper没有批量按ID查询方法
    }

    @Override
    protected Map<Long, UserRoadmapDO> getMapByIdsFromMapper(UserRoadmapMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserRoadmapMapper没有批量按ID查询方法
    }

    /**
     * 根据用户ID和路线图ID查询学习进度
     */
    @Cacheable(value = "userRoadmapByUserAndRoadmap", key = "#userId + '_' + #roadmapId")
    public UserRoadmapDO getByUserAndRoadmap(long userId, long roadmapId) {
        return userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);
    }

    /**
     * 更新学习进度
     */
    @CacheEvict(value = "userRoadmapByUserAndRoadmap", key = "#progressDO.userId + '_' + #progressDO.roadmapId")
    public void update(UserRoadmapDO progressDO) {
        userRoadmapMapper.update(progressDO);
    }

    /**
     * 删除学习进度记录
     */
    @CacheEvict(value = "userRoadmapByUserAndRoadmap", key = "#userId + '_' + #roadmapId")
    public void deleteByUserAndRoadmap(long userId, long roadmapId) {
        userRoadmapMapper.deleteByUserAndRoadmap(userId, roadmapId);
    }
}