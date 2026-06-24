package com.twicemax.content.course;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程数据服务
 * 负责课程数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDataService {

    private final CourseMapper courseMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询课程
     */
    @Cacheable(value = "courses", key = "#id", unless = "#result == null")
    public CourseDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return courseMapper.getById(id);
    }

    /**
     * 批量根据ID查询课程
     */
    public List<CourseDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return courseMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询课程并转为Map
     */
    public Map<Long, CourseDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }

    /**
     * 根据名称搜索所有状态的课程（管理后台，支持分页）
     */
    public List<CourseDO> searchByName(String name, Long lastId, int limit) {
        return courseMapper.searchByName(name, lastId, limit);
    }

    /**
     * 用户端搜索已发布的课程（简单搜索，不分页）
     */
    public List<CourseDO> searchPublishedByName(String name, int limit) {
        return courseMapper.searchPublishedByName(name, limit);
    }

    /**
     * 根据状态获取主课程列表（仅 parent_course_id=0）。state 为 NewContentState 字符串值。
     */
    public List<CourseDO> listByState(String state, Long lastId, int limit) {
        return courseMapper.listByState(state, lastId, limit);
    }

    /**
     * 根据最后ID获取课程列表（不过滤状态）
     */
    public List<CourseDO> listByLastId(Long lastId) {
        return courseMapper.listByLastId(lastId);
    }

    /**
     * 根据主分类获取根课程列表
     */
    public List<CourseDO> listRootByMainCategory(int mainCategory, Long lastId) {
        return courseMapper.listRootByMainCategory(mainCategory, lastId);
    }

    /**
     * 根据主分类和子分类获取根课程列表
     */
    public List<CourseDO> listRootByCategory(int mainCategory, int subCategory, Long lastId) {
        return courseMapper.listRootByCategory(mainCategory, subCategory, lastId);
    }

    /**
     * 根据父ID获取子课程列表
     */
    public List<CourseDO> listByParent(long parentId) {
        return courseMapper.listByParent(parentId);
    }

    /**
     * 根据父ID和状态获取子课程列表。state 为 NewContentState 字符串值。
     */
    public List<CourseDO> listByParentAndState(String state, long parentId) {
        return courseMapper.listByParentAndState(state, parentId);
    }

    /**
     * 按创建者分页（state 为 NewContentState 字符串值，可为 null 表示默认排除 BANNED）。
     */
    public List<CourseDO> listByCreator(long creatorId, Long lastId, int limit, String state) {
        return courseMapper.listByCreator(creatorId, lastId, limit, state);
    }

    /**
     * 统计活跃课程数量
     */
    public Long countActiveCourses() {
        return courseMapper.countActiveCourses();
    }

    /**
     * 统计某个父课程的已发布子课程数量
     */
    public int countPublishedSubCourses(long parentCourseId) {
        return courseMapper.countPublishedSubCourses(parentCourseId);
    }

    /**
     * 根据根节点ID查询课程
     */
    public CourseDO getByRootNodeId(long rootNodeId) {
        return courseMapper.getByRootNodeId(rootNodeId);
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

    // ==================== 验证方法 ====================

    /**
     * 验证课程ID并获取课程
     */
    public CourseDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("课程ID无效");
        }
        CourseDO course = getById(id);
        if (course == null) {
            throw StatusCode.COURSE_NOT_FOUND.exception();
        }
        return course;
    }

    /**
     * 验证课程存在
     */
    public void validateExists(Long id) {
        validateAndGet(id);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入课程
     */
    public void insert(CourseDO course) {
        courseMapper.insert(course);
    }

    /**
     * 更新课程
     */
    @CacheEvict(value = "courses", key = "#course.id")
    public void update(CourseDO course) {
        if (course == null || course.getId() == null) {
            throw new IllegalArgumentException("Course or course ID cannot be null");
        }
        courseMapper.update(course);
    }

    /**
     * 切换 pending_revision_id（提交 / 撤回 / 驳回 时用）。
     */
    @CacheEvict(value = "courses", key = "#id")
    public int updatePending(long id, Long pendingRevisionId) {
        return courseMapper.updatePending(id, pendingRevisionId);
    }

    /**
     * 审核通过：state=PUBLISHED，刷新内容镜像字段，设置 current_revision_id，清空 pending_revision_id。
     */
    @CacheEvict(value = "courses", key = "#id")
    public int approve(long id, String name, String description, String icon,
                       int mainCategory, int subCategory, long parentCourseId, long currentRevisionId) {
        return courseMapper.approve(id, name, description, icon, mainCategory, subCategory,
                parentCourseId, currentRevisionId);
    }

    /**
     * 封禁：state=BANNED，pending_revision_id 清空。
     */
    @CacheEvict(value = "courses", key = "#id")
    public int ban(long id) {
        return courseMapper.ban(id);
    }

    /**
     * 简单状态切换（解封时使用）。
     */
    @CacheEvict(value = "courses", key = "#id")
    public int updateState(long id, String state) {
        return courseMapper.updateState(id, state);
    }

    /**
     * 删除课程
     */
    @CacheEvict(value = "courses", key = "#id")
    public int delete(long id) {
        return courseMapper.delete(id);
    }

    /**
     * 增加子课程数量
     */
    @CacheEvict(value = "courses", key = "#parentCourseId")
    public int incrementSubCourseCount(long parentCourseId) {
        return courseMapper.incrementSubCourseCount(parentCourseId);
    }

    /**
     * 减少子课程数量
     */
    @CacheEvict(value = "courses", key = "#parentCourseId")
    public int decrementSubCourseCount(long parentCourseId) {
        return courseMapper.decrementSubCourseCount(parentCourseId);
    }

    /**
     * 更新子课程数量
     */
    @CacheEvict(value = "courses", key = "#id")
    public int updateSubCourseCount(long id, int count) {
        return courseMapper.updateSubCourseCount(id, count);
    }
}
