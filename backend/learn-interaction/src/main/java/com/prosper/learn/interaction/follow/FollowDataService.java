package com.prosper.learn.interaction.follow;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 关注数据服务
 */
@Service
public class FollowDataService extends AbstractDataService<FollowDO, FollowMapper, Long> {

    @Autowired
    private FollowMapper followMapper;

    @Override
    protected FollowMapper mapper() {
        return followMapper;
    }

    @Override
    protected String getCacheName() {
        return "follows";
    }

    @Override
    protected String getEntityName() {
        return "Follow";
    }

    @Override
    protected Long getEntityId(FollowDO entity) {
        return entity.getId();
    }

    @Override
    protected FollowDO getByIdFromMapper(FollowMapper mapper, Long id) {
        throw new UnsupportedOperationException();
    }

    public FollowDO getById(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FollowDO> getByIds(Collection<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, FollowDO> getMapByIds(Collection<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<FollowDO> getByIdsFromMapper(FollowMapper mapper, Collection<Long> ids) {
        return List.of(); // Follow实体不支持批量按ID查询
    }

    @Override
    protected Map<Long, FollowDO> getMapByIdsFromMapper(FollowMapper mapper, Collection<Long> ids) {
        return Map.of(); // Follow实体不支持批量按ID查询
    }

    @Override
    protected int deleteByIdFromMapper(FollowMapper mapper, Long id) {
        return 0;
    }

    /**
     * 获取关注关系
     */
    @Cacheable(value = "followRelations", key = "#followerId + '_' + #followeeId")
    public FollowDO get(long followerId, long followeeId) {
        return followMapper.get(followerId, followeeId);
    }

    /**
     * 取消关注
     */
    @CacheEvict(value = "followRelations", key = "#followerId + '_' + #followeeId")
    public void delete(long followerId, long followeeId) {
        followMapper.delete(followerId, followeeId);
    }
    
    /**
     * 插入关注关系
     */
    public void insert(Long followerId, Long followeeId) {
        followMapper.insert(followerId, followeeId);
    }
    
    /**
     * 获取关注列表
     */
    public List<FollowDO> getList(long followerId, Long lastId, int limit) {
        return followMapper.getList(followerId, lastId, limit);
    }
}