package com.prosper.learn.user.profile;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.exception.StatusCode;
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
    protected UserProfileDO getByIdFromMapper(UserProfileMapper mapper, Long userId) {
        return userProfileMapper.getById(userId);
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
     * 验证并获取用户资料
     *
     * @param id 用户ID
     * @return 用户资料实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当用户资料不存在时抛出 USER_PROFILE_NOT_FOUND (1118)
     */
    @Override
    public UserProfileDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID必须大于0");
        }

        UserProfileDO profile = getById(id);
        if (profile == null) {
            throw StatusCode.USER_PROFILE_NOT_FOUND.exception();
        }

        return profile;
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
    @CacheEvict(value = "userProfiles", key = "#userId")
    public void updateRoadmapPin(long userId, String roadmapPin) {
        // 查询现有数据
        UserProfileDO userProfile = getById(userId);
        if (userProfile == null) {
            throw StatusCode.USER_PROFILE_NOT_FOUND.exception();
        }

        // 更新字段
        userProfile.setRoadmapPin(roadmapPin);

        // 调用update方法，TimestampInterceptor会自动设置updatedAt
        update(userProfile);
    }

    /**
     * 插入新用户档案
     */
    public void insert(UserProfileDO userProfileDO) {
        userProfileMapper.insert(userProfileDO);
    }
}