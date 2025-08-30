package com.prosper.learn.domain.service.data;

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
    
    /**
     * 根据主分类和子分类获取课程列表（带缓存）
     */
    @Cacheable(value = "coursesByCategory", key = "#mainCategory + '_' + #subCategory")
    public List<CourseDO> getByCategory(int mainCategory, int subCategory) {
        try {
            return courseMapper.listRootByCategory(mainCategory, subCategory);
        } catch (Exception e) {
            log.error("Error querying courses by category: {}, {}", mainCategory, subCategory, e);
            throw new RuntimeException("Failed to query courses by category", e);
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
            throw new RuntimeException("Failed to update course: " + course.getId(), e);
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
    public boolean approve(long id) {
        try {
            int result = courseMapper.approve(id);
            return result > 0;
        } catch (Exception e) {
            log.error("Error approving course: {}", id, e);
            throw new RuntimeException("Failed to approve course: " + id, e);
        }
    }
    
    /**
     * 课程拒绝
     */
    @CacheEvict(value = "courses", key = "#id")
    public boolean reject(long id, String rejectedReason) {
        try {
            int result = courseMapper.reject(id, rejectedReason);
            return result > 0;
        } catch (Exception e) {
            log.error("Error rejecting course: {}", id, e);
            throw new RuntimeException("Failed to reject course: " + id, e);
        }
    }
}