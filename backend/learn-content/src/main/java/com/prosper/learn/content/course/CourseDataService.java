package com.prosper.learn.content.course;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程数据服务，提供缓存功能
 * 专注于数据访问和缓存管理，避免循环依赖
 *
 * 按照CQRS模式分离Command和Query操作
 */
@Slf4j
@Service
public class CourseDataService extends AbstractDataService<CourseDO, CourseMapper, Long> {

    @Autowired
    private CourseMapper courseMapper;

    // ========== AbstractDataService 实现 ==========

    @Override
    protected CourseMapper mapper() {
        return courseMapper;
    }

    @Override
    protected String getCacheName() {
        return "courses";
    }

    @Override
    protected String getEntityName() {
        return "Course";
    }

    @Override
    protected Long getEntityId(CourseDO entity) {
        return entity.getId();
    }

    @Override
    protected CourseDO getByIdFromMapper(CourseMapper mapper, Long id) {
        return mapper.getById(id);
    }

    @Override
    protected List<CourseDO> getByIdsFromMapper(CourseMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }

    @Override
    protected Map<Long, CourseDO> getMapByIdsFromMapper(CourseMapper mapper, Collection<Long> ids) {
        return getByIdsFromMapper(mapper, ids).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }

    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(15);
    }

    @Override
    protected int deleteByIdFromMapper(CourseMapper mapper, Long id) {
        return 0;
    }

    /**
     * 验证课程ID并获取课程实体
     * 重写父类方法以抛出更具体的 COURSE_NOT_FOUND 异常
     *
     * @param id 课程ID
     * @return 课程实体
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当课程不存在时抛出 COURSE_NOT_FOUND (1201)
     */
    @Override
    public CourseDO validateAndGet(Long id) {
        if (id == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程ID不能为空");
        }

        CourseDO course = getById(id);
        if (course == null) {
            throw StatusCode.COURSE_NOT_FOUND.exception();
        }

        return course;
    }

    // ========== QUERY 查询操作 ==========

    /**
     * 根据名称搜索课程（不缓存）
     */
    public List<CourseDO> searchByName(String name, int limit) {
        return courseMapper.searchByName(name, limit);
    }

    private static final int DEFAULT_PAGE_SIZE = 21;

    /**
     * 根据状态获取课程列表
     */
    public List<CourseDO> listByState(Byte state, Long lastId, int limit) {
        return courseMapper.listByState(state, lastId, limit);
    }

    /**
     * 根据最后ID获取课程列表（不缓存，不过滤状态）
     */
    public List<CourseDO> listByLastId(Long lastId) {
        return courseMapper.listByLastId(lastId);
    }

    /**
     * 根据主分类获取根课程列表（不缓存，支持分页）
     */
    public List<CourseDO> listRootByMainCategory(int mainCategory, Long lastId) {
        return courseMapper.listRootByMainCategory(mainCategory, lastId);
    }

    /**
     * 根据主分类和子分类获取根课程列表（不缓存，支持分页）
     */
    public List<CourseDO> listRootByCategory(int mainCategory, int subCategory, Long lastId) {
        return courseMapper.listRootByCategory(mainCategory, subCategory, lastId);
    }

    /**
     * 根据父ID获取子课程列表（不缓存）
     */
    public List<CourseDO> listByParent(long parentId) {
        return courseMapper.listByParent(parentId);
    }

    /**
     * 根据父ID和状态获取子课程列表（不缓存）
     */
    public List<CourseDO> listByParentAndState(Enums.ContentState state, long parentId) {
        return courseMapper.listByParentAndState(state, parentId);
    }

    /**
     * 统计活跃课程数量
     */
    public Long countActiveCourses() {
        return courseMapper.countActiveCourses();
    }

    /**
     * 根据根节点ID列表批量查询课程
     */
    public List<CourseDO> getByRootNodeIds(List<Long> rootNodeIds) {
        if (rootNodeIds == null || rootNodeIds.isEmpty()) {
            return List.of();
        }
        return courseMapper.getByRootNodeIds(rootNodeIds);
    }

    // ========== COMMAND 命令操作 ==========

    /**
     * 插入课程（不清除缓存，新数据不影响现有缓存）
     */
    public void insert(CourseDO course) {
        courseMapper.insert(course);
    }

    /**
     * 更新课程并清除缓存
     */
    @CacheEvict(value = "courses", key = "#course.id")
    public void update(CourseDO course) {
        if (course == null || course.getId() == null) {
            throw new IllegalArgumentException("Course or course ID cannot be null");
        }

        try {
            courseMapper.update(course);
            log.debug("Updated course {}", course.getId());
        } catch (Exception e) {
            log.error("Error updating course: {}", course.getId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }

    /**
     * 课程审批
     */
    @CacheEvict(value = "courses", key = "#id")
    public int approve(long id) {
        return courseMapper.approve(id);
    }

    /**
     * 课程拒绝
     */
    @CacheEvict(value = "courses", key = "#id")
    public int reject(long id, String reason) {
        return courseMapper.reject(id, reason);
    }

    /**
     * 课程封禁
     */
    @CacheEvict(value = "courses", key = "#id")
    public int ban(long id, String reason) {
        return courseMapper.ban(id, reason);
    }

    /**
     * 删除课程
     */
    @CacheEvict(value = "courses", key = "#id")
    public int delete(long id) {
        return courseMapper.delete(id);
    }
}