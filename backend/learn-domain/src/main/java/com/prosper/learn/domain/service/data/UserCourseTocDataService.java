package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UserCourseTocDO;
import com.prosper.learn.persistence.mapper.UserCourseTocMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户课程目录数据服务
 */
@Service
public class UserCourseTocDataService extends AbstractDataService<UserCourseTocDO, UserCourseTocMapper, Long> {

    @Autowired
    private UserCourseTocMapper userCourseTocMapper;

    @Override
    protected UserCourseTocMapper mapper() {
        return userCourseTocMapper;
    }

    @Override
    protected String getCacheName() {
        return "userCourseTocs";
    }

    @Override
    protected String getEntityName() {
        return "UserCourseToc";
    }

    @Override
    protected Long getEntityId(UserCourseTocDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCourseTocDO getByIdFromMapper(UserCourseTocMapper mapper, Long id) {
        return userCourseTocMapper.get(id);
    }

    @Override
    protected List<UserCourseTocDO> getByIdsFromMapper(UserCourseTocMapper mapper, Collection<Long> ids) {
        return List.of(); // UserCourseTocMapper没有批量查询方法
    }

    @Override
    protected Map<Long, UserCourseTocDO> getMapByIdsFromMapper(UserCourseTocMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserCourseTocMapper没有批量查询方法
    }

    /**
     * 根据用户ID和课程ID查询用户课程目录
     */
    @Cacheable(value = "userCourseTocByUserAndCourse", key = "#userId + '_' + #courseId")
    public UserCourseTocDO getByUserAndCourse(long userId, long courseId) {
        return userCourseTocMapper.getByUserAndCourse(userId, courseId);
    }

    /**
     * 更新用户课程目录
     */
    @CacheEvict(value = "userCourseTocByUserAndCourse", key = "#userCourseTocDO.userId + '_' + #userCourseTocDO.courseId")
    public void update(UserCourseTocDO userCourseTocDO) {
        userCourseTocMapper.update(userCourseTocDO);
    }

    /**
     * 删除用户课程目录记录
     */
    @CacheEvict(value = "userCourseTocByUserAndCourse", key = "#userCourseTocDO.userId + '_' + #userCourseTocDO.courseId")
    public void delete(UserCourseTocDO userCourseTocDO) {
        userCourseTocMapper.delete(userCourseTocDO.getId());
    }
}