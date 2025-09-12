package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 投票数据服务
 */
@Service
public class UpvoteDataService extends AbstractDataService<UpvoteDO, UpvoteMapper, Long> {

    @Autowired
    private UpvoteMapper upvoteMapper;

    @Override
    protected UpvoteMapper mapper() {
        return upvoteMapper;
    }

    @Override
    protected String getCacheName() {
        return "upvotes";
    }

    @Override
    protected String getEntityName() {
        return "Upvote";
    }

    @Override
    protected Long getEntityId(UpvoteDO entity) {
        return entity.getId();
    }

    @Override
    protected UpvoteDO getByIdFromMapper(UpvoteMapper mapper, Long id) {
        return upvoteMapper.getById(id);
    }

    @Override
    protected List<UpvoteDO> getByIdsFromMapper(UpvoteMapper mapper, Collection<Long> ids) {
        return List.of(); // 投票实体不支持批量按ID查询
    }

    @Override
    protected Map<Long, UpvoteDO> getMapByIdsFromMapper(UpvoteMapper mapper, Collection<Long> ids) {
        return Map.of(); // 投票实体不支持批量按ID查询
    }

    @Override
    protected int deleteByIdFromMapper(UpvoteMapper mapper, Long id) {
        return 0;
    }

    /**
     * 获取用户对特定对象的投票
     */
    @Cacheable(value = "upvotesByUser", key = "#userId + '_' + #objectId + '_' + #objectType")
    public UpvoteDO getByUserAndObject(long userId, long objectId, int objectType) {
        return upvoteMapper.getByUserAndObject(userId, objectId, objectType);
    }

    /**
     * 更新投票记录
     */
    @CacheEvict(value = "upvotesByUser", key = "#upvoteDO.userId + '_' + #upvoteDO.objectId + '_' + #upvoteDO.objectType")
    public void update(UpvoteDO upvoteDO) {
        upvoteMapper.update(upvoteDO);
    }

    /**
     * 删除投票记录（先查询获取信息，再删除并清除缓存）
     */
    public void delete(long id) {
        // 先查询获取完整信息
        UpvoteDO upvote = upvoteMapper.getById(id);
        if (upvote != null) {
            // 清除对应缓存
            evictUserUpvoteCache(upvote.getUserId(), upvote.getObjectId(), upvote.getObjectType());
            // 删除数据
            upvoteMapper.delete(id);
        }
    }

    /**
     * 手动清除用户投票缓存
     */
    @CacheEvict(value = "upvotesByUser", key = "#userId + '_' + #objectId + '_' + #objectType")
    public void evictUserUpvoteCache(long userId, long objectId, int objectType) {
        // 缓存清除由注解自动处理
    }
    
    /**
     * 获取用户的投票列表（不缓存）
     */
    public List<UpvoteDO> getList(long userId, List<Long> objectIds, int objectType) {
        return upvoteMapper.getList(userId, objectIds, objectType);
    }

    /**
     * 插入新的投票记录
     */
    public void insert(UpvoteDO upvoteDO) {
        upvoteMapper.insert(upvoteDO);
    }
}