package com.prosper.learn.business.service.data;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
 */
@Slf4j
@Service
public class CourseDataService extends AbstractDataService<CourseDO, CourseMapper, Long> {
    
    @Autowired
    private CourseMapper courseMapper;
    
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
     * 根据主分类和子分类获取课程列表（带缓存）
     */
    @Cacheable(value = "coursesByCategory", key = "#mainCategory + '_' + #subCategory")
    public List<CourseDO> getByCategory(int mainCategory, int subCategory) {
        try {
            return courseMapper.listRootByCategory(mainCategory, subCategory, null);
        } catch (Exception e) {
            log.error("Error querying courses by category: {}, {}", mainCategory, subCategory, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
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
            // 清除分类缓存
            evictCategoryCache(course.getMainCategory(), course.getSubCategory());
            log.debug("Updated course {}", course.getId());
        } catch (Exception e) {
            log.error("Error updating course: {}", course.getId(), e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 清除分类缓存
     */
    @CacheEvict(value = "coursesByCategory", key = "#mainCategory + '_' + #subCategory")
    public void evictCategoryCache(int mainCategory, int subCategory) {
        log.debug("Evicted category cache for: {}_{}", mainCategory, subCategory);
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
     * 根据名称搜索课程（不缓存）
     */
    public List<CourseDO> searchByName(String name, int limit) {
        return courseMapper.searchByName(name, limit);
    }
    
    /**
     * 根据状态和最后ID获取课程列表（不缓存）
     */
    public List<CourseDO> listByStateAndLastId(ContentState state, Long lastId) {
        return courseMapper.listByStateAndLastId(state, lastId);
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
    public List<CourseDO> listByParent(Long parentId) {
        return courseMapper.listByParent(parentId);
    }
    
    /**
     * 根据父ID和状态获取子课程列表（不缓存）
     */
    public List<CourseDO> listByParentAndState(ContentState state, Long parentId) {
        return courseMapper.listByParentAndState(state, parentId);
    }
    
    /**
     * 删除课程
     */
    @CacheEvict(value = "courses", key = "#id")
    public int delete(Long id) {
        return courseMapper.delete(id);
    }
    
    /**
     * 插入课程（不清除缓存，新数据不影响现有缓存）
     */
    public void insert(CourseDO course) {
        courseMapper.insert(course);
    }
    
    /**
     * 统计活跃课程数量
     */
    public Long countActiveCourses() {
        return courseMapper.countActiveCourses();
    }
}