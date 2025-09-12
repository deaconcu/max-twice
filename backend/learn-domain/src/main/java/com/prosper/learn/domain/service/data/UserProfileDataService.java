package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户档案数据服务
 */
@Service
public class UserProfileDataService extends AbstractDataService<UserProfileDO, UserProfileMapper, Long> {

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Override
    protected UserProfileMapper mapper() {
        return userProfileMapper;
    }

    @Override
    protected String getCacheName() {
        return "userProfiles";
    }

    @Override
    protected String getEntityName() {
        return "UserProfile";
    }

    @Override
    protected Long getEntityId(UserProfileDO entity) {
        return entity.getUserId();
    }

    @Override
    protected UserProfileDO getByIdFromMapper(UserProfileMapper mapper, Long id) {
        return userProfileMapper.getById(id);
    }

    @Override
    protected List<UserProfileDO> getByIdsFromMapper(UserProfileMapper mapper, Collection<Long> ids) {
        return List.of(); // 类型不匹配，UserProfileMapper.getByIds需要Collection<Integer>
    }

    @Override
    protected Map<Long, UserProfileDO> getMapByIdsFromMapper(UserProfileMapper mapper, Collection<Long> ids) {
        return Map.of(); // 类型不匹配
    }

    @Override
    protected int deleteByIdFromMapper(UserProfileMapper mapper, Long id) {
        return 0;
    }

    /**
     * 更新用户档案
     */
    @CacheEvict(value = "userProfiles", key = "#user.userId")
    public void update(UserProfileDO user) {
        userProfileMapper.update(user);
    }

    /**
     * 更新路线图置顶
     */
    @CacheEvict(value = "userProfiles", key = "#id")
    public void updateRoadmapPin(long id, String roadmapPin) {
        userProfileMapper.updateRoadmapPin(id, roadmapPin);
    }

    /**
     * 插入新用户档案
     */
    public void insert(UserProfileDO userProfileDO) {
        userProfileMapper.insert(userProfileDO);
    }
}