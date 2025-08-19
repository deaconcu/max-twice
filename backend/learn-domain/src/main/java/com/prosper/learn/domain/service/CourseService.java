package com.prosper.learn.domain.service;

import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CourseDTO;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final NodeMapper nodeMapper;
    private final CourseRankingService courseRankingService;

    public boolean exist(int id) {
        CourseDO courseDO = courseMapper.getById(id);
        return courseDO != null;
    }

    public CourseDTOV4 getById(int id) {
        CourseDO courseDO = courseMapper.getById(id);
        return courseDO != null ? Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseMapper) : null;
    }

    // 新增：根据状态和lastId获取课程列表
    public List<CourseDTOV4> getListByStateAndLastId(String state, int lastId) {
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
    public List<CourseDTOV4> getListByParent(int parentId, String state) {
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

    // 新增：课程批准操作
    public void approve(int id) {
        // 先查询当前状态
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw new RuntimeException("操作失败：课程不存在");
        }
        if ("APPROVED".equals(courseDO.getState())) {
            throw new RuntimeException("操作失败：课程状态已是批准状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = courseMapper.approve(id);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：课程状态已被其他操作修改，请刷新后重试");
        }
    }

    // 新增：课程拒绝操作
    public void reject(int id, String rejectedReason) {
        // 先查询当前状态
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw new RuntimeException("操作失败：课程不存在");
        }
        if ("REJECTED".equals(courseDO.getState())) {
            throw new RuntimeException("操作失败：课程状态已是被屏蔽状态，无需重复操作");
        }

        // 执行数据库操作，再次验证状态（防止并发问题）
        int rowsAffected = courseMapper.reject(id, rejectedReason);
        if (rowsAffected == 0) {
            throw new RuntimeException("操作失败：课程状态已被其他操作修改，请刷新后重试");
        }
    }

    public void delete(int id) {
        CourseDO courseDO = courseMapper.getById(id);
        if (courseDO == null) {
            throw new RuntimeException("操作失败：课程不存在");
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
        Integer parentId = 0;
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
        course.setState("SUBMITTED");
        course.setRootNode(0);
        courseMapper.insert(course);

        NodeDO nodeDO = NodeDO.createRoot(course.getCreator(), course.getId());
        nodeMapper.insert(nodeDO);

        course.setRootNode(nodeDO.getId());
        courseMapper.update(course);
    }

    public void createSubcourse(String name, String description, int parentId, int userId) {
        CourseDO parentCourse = courseMapper.getById(parentId);
        if (parentCourse == null) {
            throw new RuntimeException("Parent course does not exist");
        }

        CourseDO subCourse = new CourseDO();
        subCourse.setName(name);
        subCourse.setDescription(description);
        subCourse.setCreator(userId);
        subCourse.setParent(parentId);
        subCourse.setState("SUBMITTED");
        subCourse.setRootNode(0); // 子课程的 rootNode 初始为 0
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
            List<Integer> hotCourseIds = courseRankingService.getHotCourseIds(limit);
            
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
            List<Integer> hotCourseIds = courseRankingService.getHotCourseIds(100);
            
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
