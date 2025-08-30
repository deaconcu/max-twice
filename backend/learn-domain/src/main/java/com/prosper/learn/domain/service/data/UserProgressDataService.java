package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UserProgressDO;
import com.prosper.learn.persistence.mapper.UserProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户学习进度数据服务
 */
@Service
public class UserProgressDataService extends AbstractDataService<UserProgressDO, UserProgressMapper, Long> {

    @Autowired
    private UserProgressMapper userProgressMapper;

    @Override
    protected UserProgressMapper mapper() {
        return userProgressMapper;
    }

    @Override
    protected String getCacheName() {
        return "userProgress";
    }

    @Override
    protected String getEntityName() {
        return "UserProgress";
    }

    @Override
    protected Long getEntityId(UserProgressDO entity) {
        return entity.getUserId();
    }

    @Override
    protected UserProgressDO getByIdFromMapper(UserProgressMapper mapper, Long id) {
        return userProgressMapper.getByUserId(id);
    }

    @Override
    protected List<UserProgressDO> getByIdsFromMapper(UserProgressMapper mapper, Collection<Long> ids) {
        return List.of(); // UserProgressMapper没有批量查询方法
    }

    @Override
    protected Map<Long, UserProgressDO> getMapByIdsFromMapper(UserProgressMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserProgressMapper没有批量查询方法
    }

    /**
     * 根据用户ID获取学习进度
     */
    @Cacheable(value = "userProgress", key = "#userId")
    public UserProgressDO getByUserId(long userId) {
        return userProgressMapper.getByUserId(userId);
    }

    /**
     * 插入或更新学习进度
     */
    @CacheEvict(value = "userProgress", key = "#record.userId")
    public int upsert(UserProgressDO record) {
        return userProgressMapper.upsert(record);
    }
}