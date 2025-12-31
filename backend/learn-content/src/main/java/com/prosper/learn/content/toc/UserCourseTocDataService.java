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

    @Override
    protected int deleteByIdFromMapper(UserCourseTocMapper mapper, Long id) {
        return 0;
    }

    /**
     * 验证并获取用户课程目录
     *
     * @param id 用户课程目录ID
     * @return 用户课程目录实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当用户课程目录不存在时抛出 TOC_USER_TOC_NOT_FOUND (1801)
     */
    @Override
    public UserCourseTocDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户课程目录ID不能为空");
        }

        if (id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户课程目录ID必须大于0");
        }

        UserCourseTocDO toc = getById(id);
        if (toc == null) {
            throw StatusCode.TOC_USER_TOC_NOT_FOUND.exception();
        }

        return toc;
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

    /**
     * 插入用户课程目录记录
     */
    public void insert(UserCourseTocDO userCourseTocDO) {
        userCourseTocMapper.insert(userCourseTocDO);
    }
}