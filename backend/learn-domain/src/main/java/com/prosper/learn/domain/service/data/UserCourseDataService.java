package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.mapper.UserCourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用户课程数据服务
 */
@Service
public class UserCourseDataService extends AbstractDataService<UserCourseDO, UserCourseMapper, Long> {

    @Autowired
    private UserCourseMapper userCourseMapper;

    @Override
    protected UserCourseMapper mapper() {
        return userCourseMapper;
    }

    @Override
    protected String getCacheName() {
        return "userCourses";
    }

    @Override
    protected String getEntityName() {
        return "UserCourse";
    }

    @Override
    protected Long getEntityId(UserCourseDO entity) {
        return entity.getId();
    }

    @Override
    protected UserCourseDO getByIdFromMapper(UserCourseMapper mapper, Long id) {
        return null; // UserCourseMapper没有getById方法
    }

    @Override
    protected List<UserCourseDO> getByIdsFromMapper(UserCourseMapper mapper, Collection<Long> ids) {
        return List.of(); // UserCourseMapper没有批量按ID查询方法
    }

    @Override
    protected Map<Long, UserCourseDO> getMapByIdsFromMapper(UserCourseMapper mapper, Collection<Long> ids) {
        return Map.of(); // UserCourseMapper没有批量按ID查询方法
    }

    @Override
    protected int deleteByIdFromMapper(UserCourseMapper mapper, Long id) {
        return 0;
    }

    /**
     * 根据用户ID和课程ID查询进度
     */
    @Cacheable(value = "userCourseByUserAndCourse", key = "#userId + '_' + #courseId")
    public UserCourseDO getByUserIdAndCourseId(long userId, long courseId) {
        return userCourseMapper.getByUserIdAndCourseId(userId, courseId);
    }

    /**
     * 更新用户课程进度
     */
    @CacheEvict(value = "userCourseByUserAndCourse", key = "#userCourseDO.userId + '_' + #userCourseDO.courseId")
    public int update(UserCourseDO userCourseDO) {
        return userCourseMapper.update(userCourseDO);
    }

    /**
     * 删除用户课程记录
     */
    @CacheEvict(value = "userCourseByUserAndCourse", key = "#userCourseDO.userId + '_' + #userCourseDO.courseId")
    public void delete(UserCourseDO userCourseDO) {
        userCourseMapper.delete(userCourseDO.getId());
    }

    /**
     * 删除用户课程记录（推荐使用此方法，可以自动清除缓存）
     */
    @CacheEvict(value = "userCourseByUserAndCourse", key = "#userId + '_' + #courseId")
    public void deleteByUserAndCourse(long userId, long courseId) {
        userCourseMapper.deleteByUserAndCourse(userId, courseId);
    }
    
    /**
     * 插入用户课程记录
     */
    public void insert(UserCourseDO userCourseDO) {
        userCourseMapper.insert(userCourseDO);
    }

    /**
     * 根据用户ID获取课程列表
     */
    public List<UserCourseDO> getByUserId(long userId, long lastId) {
        return userCourseMapper.getByUserId(userId, lastId);
    }

    /**
     * 根据用户ID和课程ID列表获取课程映射
     */
    public Map<Long, UserCourseDO> getByUserIdAndCourseIdsAsMap(long userId, List<Long> courseIds) {
        return userCourseMapper.getByUserIdAndCourseIdsAsMap(userId, courseIds);
    }
}