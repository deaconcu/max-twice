package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CourseDTO;
import com.prosper.learn.dto.CourseDTOV3;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.prosper.learn.common.Enums.*;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final NodeMapper nodeMapper;
    private final CourseRankingService courseRankingService;

    public CourseDTOV4 getCourseById(Long id) {
        CourseDO course = courseMapper.getById(id);
        if (course == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        return Converter.INSTANCE.toCourseDTOV4(course);
    }

    public List<CourseDTOV3> searchCoursesByName(String name) {
        List<CourseDO> courseList = courseMapper.searchByName(name, 20);
        return Converter.INSTANCE.toCourseDTOV3(courseList);
    }

    @Transactional
    public void updateCourse(Long id, CourseDTO courseDTO) {
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }

        courseDO.setName(courseDTO.getName());
        courseDO.setDescription(courseDTO.getDescription());
        courseMapper.update(courseDO);
    }

    @Cacheable(value = "cs", key = "#id")
    public CourseDO getCourseDOById(int id) {
        return courseMapper.getById(id);
    }

    public boolean exist(long id) {
        CourseDO courseDO = courseMapper.getById(id);
        return courseDO != null;
    }

    public CourseDTOV4 getById(int id) {
        CourseDO courseDO = courseMapper.getById(id);
        return courseDO != null ? Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper) : null;
    }

    // 新增：根据状态和lastId获取课程列表
    public List<CourseDTOV4> getListByStateAndLastId(String state, long lastId) {
        List<CourseDO> courseDOList = courseMapper.listByStateAndLastId(state, lastId);
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper))
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据主分类和子分类获取已批准的课程列表
    public List<CourseDTOV4> getListByCategory(int mainCategory, int subCategory) {
        List<CourseDO> courseDOList;
        courseDOList = courseMapper.listRootByCategory(mainCategory, subCategory);
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper))
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据父课程ID获取子课程列表
    public List<CourseDTOV4> getListByParent(long parentId, String state) {
        List<CourseDO> courseDOList;
        if (state.equals("ALL")) {
            courseDOList = courseMapper.listByParent(parentId);
        } else {
            courseDOList = courseMapper.listByParentAndState(state, parentId);
        }
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper))
                .collect(java.util.stream.Collectors.toList());
    }

    public void approve(long id) {
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        if ("APPROVED".equals(courseDO.getState())) {
            throw new RuntimeException("操作失败：课程状态已是批准状态，无需重复操作");
        }

        int rowsAffected = courseMapper.approve(id);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：课程状态已被其他操作修改，请刷新后重试");
        }
    }

    public void reject(long id, String rejectedReason) {
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        if ("REJECTED".equals(courseDO.getState())) {
            throw new RuntimeException("操作失败：课程状态已是被屏蔽状态，无需重复操作");
        }

        int rowsAffected = courseMapper.reject(id, rejectedReason);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：课程状态已被其他操作修改，请刷新后重试");
        }
    }

    public void delete(long id) {
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }

        int rowsAffected = courseMapper.delete(id);
        if (rowsAffected == 0) {
            throw new RuntimeException("删除失败");
        }
    }

    public void createCourse(CourseDTO courseDTO) {
        CourseDO course = new CourseDO();
        course.setName(courseDTO.getName());
        course.setDescription(courseDTO.getDescription());

        // 修正：parent 现在是对象类型，需要获取其 id
        long parentId = 0;
        if (courseDTO.getParentId() != null && courseDTO.getParentId() <= 0) {
            parentId = courseDTO.getParentId();
        }
        course.setParent(parentId);

        if (parentId > 0) {
            CourseDO parentCourse = courseMapper.getById(parentId);
            if (parentCourse == null) {
                throw new RuntimeException("parent course is not exist");
            }
        }

        course.setCreator(courseDTO.getCreator());
        course.setMainCategory(courseDTO.getMainCategory());
        course.setSubCategory(courseDTO.getSubCategory());
        course.setState(CourseState.SUBMITTED.value());
        course.setRootNode(0L);
        courseMapper.insert(course);

        NodeDO nodeDO = NodeDO.createRoot(course.getCreator(), course.getId());
        nodeMapper.insert(nodeDO);

        course.setRootNode(nodeDO.getId());
        courseMapper.update(course);
    }

    public void createSubcourse(String name, String description, long parentId, long userId) {
        CourseDO parentCourse = courseMapper.getById(parentId);
        if (parentCourse == null) {
            throw new RuntimeException("Parent course does not exist");
        }

        CourseDO subCourse = new CourseDO();
        subCourse.setName(name);
        subCourse.setDescription(description);
        subCourse.setCreator(userId);
        subCourse.setParent(parentId);
        subCourse.setState(CourseState.SUBMITTED.value());
        subCourse.setRootNode(0L); // 子课程的 rootNode 初始为 0
        subCourse.setMainCategory(parentCourse.getMainCategory());
        subCourse.setSubCategory(parentCourse.getSubCategory());

        courseMapper.insert(subCourse);

        NodeDO nodeDO = NodeDO.createRoot(userId, subCourse.getId());
        nodeMapper.insert(nodeDO);

        subCourse.setRootNode(nodeDO.getId());
        courseMapper.update(subCourse);
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseDTOV4> getHotCourses(int limit) {
        try {
            // 从Redis获取热门课程ID列表
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(limit);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 根据ID列表获取课程详情
            List<CourseDO> courseDOList = courseMapper.getByIds(hotCourseIds);
            
            // 转换为DTO并附加统计信息
            List<CourseDTOV4> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                CourseDTOV4 courseDTO = Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper);
                
                // 从Redis获取统计数据
                CourseRankingService.CourseStats stats = courseRankingService.getCourseStats(courseDO.getId());
                
                // 将统计数据添加到DTO中（需要在DTO中添加相应字段）
                // 这里暂时使用description字段存储统计信息，实际项目中应该在DTO中添加专门的字段
                courseDTO.setLearnerCount((int) stats.getLearningCount());
                courseDTO.setSubscriptionCount((int) stats.getSubscriptionCount());
                
                result.add(courseDTO);
            }
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取热门课程失败: " + e.getMessage(), e);
        }
    }
    
    // 获取热门课程完整排行榜（前100名）
    public List<CourseDTOV4> getHotCoursesRanking() {
        try {
            // 获取前100名热门课程
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(100);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 根据ID列表获取课程详情
            List<CourseDO> courseDOList = courseMapper.getByIds(hotCourseIds);
            
            // 转换为DTO并附加统计信息
            List<CourseDTOV4> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                CourseDTOV4 courseDTO = Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper);
                
                // 从Redis获取统计数据
                CourseRankingService.CourseStats stats = courseRankingService.getCourseStats(courseDO.getId());
                
                courseDTO.setLearnerCount((int) stats.getLearningCount());
                courseDTO.setSubscriptionCount((int) stats.getSubscriptionCount());
                
                result.add(courseDTO);
            }
            
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取热门课程排行榜失败: " + e.getMessage(), e);
        }
    }
}
