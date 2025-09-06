package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.Enums.CourseState;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.domain.service.converter.CourseConverter;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.request.CreateCourseRequest;
import com.prosper.learn.dto.request.UpdateCourseRequest;
import com.prosper.learn.dto.response.old.CourseDTOV1;
import com.prosper.learn.dto.response.old.CourseDTOV3;
import com.prosper.learn.dto.response.old.CourseDTOV4;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.NodeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseDataService courseDataService;
    private final NodeDataService nodeDataService;
    private final CourseRankingService courseRankingService;
    private final SystemProperties systemProperties;
    private final CourseConverter courseConverter;

    // ========== 私有辅助方法 ==========

    /**
     * 验证课程是否存在
     */
    private CourseDO validateCourseExists(long courseId) {
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        return courseDO;
    }

    /**
     * 验证父课程是否存在
     */
    private void validateParentCourseExists(long parentId) {
        if (!systemProperties.getCourse().isEnableParentValidation()) {
            return;
        }
        if (parentId > 0) {
            CourseDO parentCourse = courseDataService.getById(parentId);
            if (parentCourse == null) {
                throw ErrorCode.COURSE_PARENT_NOT_FOUND.exception();
            }
        }
    }

    /**
     * 验证课程状态并检查重复操作
     */
    private void validateCourseStateForApproval(CourseDO courseDO) {
        if (!systemProperties.getCourse().isEnableStateValidation()) {
            return;
        }
        if (CourseState.APPROVED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_APPROVED.exception();
        }
    }

    /**
     * 验证课程状态并检查重复操作
     */
    private void validateCourseStateForRejection(CourseDO courseDO) {
        if (!systemProperties.getCourse().isEnableStateValidation()) {
            return;
        }
        if (CourseState.REJECTED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_REJECTED.exception();
        }
    }

    /**
     * 验证数据库操作结果
     */
    private void validateOperationResult(int rowsAffected) {
        if (rowsAffected == 0) {
            throw ErrorCode.COURSE_STATE_CONFLICT.exception();
        }
    }

    /**
     * 转换为DTO并附加统计信息
     */
    private CourseDTOV1 convertToDTOWithStats(CourseDO courseDO) {
        //CourseDTOV4 courseDTO = Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService);
        CourseDTOV1 courseDTOV1 = courseConverter.toCourseDTOV4(courseDO);

        try {
            CourseRankingService.CourseStats stats = courseRankingService.getCourseStats(courseDO.getId());
            courseDTOV1.setLearnerCount((int) stats.getLearningCount());
            courseDTOV1.setSubscriptionCount((int) stats.getSubscriptionCount());
        } catch (Exception e) {
            // 统计信息获取失败时设置默认值
            courseDTOV1.setLearnerCount(0);
            courseDTOV1.setSubscriptionCount(0);
        }
        
        return courseDTOV1;
    }

    // ========== 公共业务方法 ==========

    public CourseDTOV4 getCourseById(Long id) {
        CourseDO course = validateCourseExists(id);
        return Converter.INSTANCE.toCourseDTOV4(course);
    }

    public List<CourseDTOV3> searchCoursesByName(String name) {
        int searchLimit = systemProperties.getCourse().getSearchLimit();
        List<CourseDO> courseList = courseDataService.searchByName(name, searchLimit);
        return Converter.INSTANCE.toCourseDTOV3(courseList);
    }

    @Transactional
    public void updateCourse(Long id, UpdateCourseRequest request) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("课程更新请求不能为空");
        }
        
        CourseDO courseDO = validateCourseExists(id);
        
        courseDO.setName(request.getName());
        courseDO.setDescription(request.getDescription());
        courseDO.setMainCategory(request.getMainCategory());
        courseDO.setSubCategory(request.getSubCategory());
        courseDataService.update(courseDO);
    }

    @Cacheable(value = "cs", key = "#id")
    public CourseDO getCourseDOById(long id) {
        return courseDataService.getById(id);
    }

    public boolean exist(long id) {
        CourseDO courseDO = courseDataService.getById(id);
        return courseDO != null;
    }

    public CourseDTOV4 getById(long id) {
        CourseDO courseDO = courseDataService.getById(id);
        return courseDO != null ? Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService) : null;
    }

    // 新增：根据状态和lastId获取课程列表
    public List<CourseDTOV4> getListByStateAndLastId(CourseState state, long lastId) {
        List<CourseDO> courseDOList = courseDataService.listByStateAndLastId(state, lastId);
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService))
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据主分类和子分类获取已批准的课程列表
    public List<CourseDTOV4> getListByCategory(int mainCategory, int subCategory) {
        List<CourseDO> courseDOList;
        courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory);
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService))
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据父课程ID获取子课程列表
    public List<CourseDTOV4> getListByParent(long parentId, CourseState state) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return courseDOList.stream()
                .map(courseDO -> Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService))
                .collect(java.util.stream.Collectors.toList());
    }

    public void approve(long id) {
        CourseDO courseDO = validateCourseExists(id);
        validateCourseStateForApproval(courseDO);

        int rowsAffected = courseDataService.approve(id);
        validateOperationResult(rowsAffected);
    }

    public void reject(long id, String rejectedReason) {
        CourseDO courseDO = validateCourseExists(id);
        validateCourseStateForRejection(courseDO);

        int rowsAffected = courseDataService.reject(id, rejectedReason);
        validateOperationResult(rowsAffected);
    }

    public void delete(long id) {
        validateCourseExists(id);

        int rowsAffected = courseDataService.delete(id);
        if (rowsAffected == 0) {
            throw ErrorCode.COURSE_DELETE_FAILED.exception();
        }
    }

    public void createCourse(CreateCourseRequest request, Long userId) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("课程创建请求不能为空");
        }
        if (userId == null || userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效");
        }

        long parentId = 0;
        validateParentCourseExists(parentId);

        // 验证通过后创建对象
        CourseDO course = new CourseDO();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setMainCategory(request.getMainCategory());
        course.setSubCategory(request.getSubCategory());
        course.setCreatorId(userId);
        course.setParentCourseId(parentId);
        course.setState(CourseState.SUBMITTED.value());
        course.setRootNodeId(0L);
        
        courseDataService.insert(course);

        NodeDO nodeDO = NodeDO.createRoot(userId, course.getId());
        nodeDataService.insert(nodeDO);

        course.setRootNodeId(nodeDO.getId());
        courseDataService.update(course);
    }

    public void createSubcourse(String name, String description, long parentId, long userId) {
        validateParentCourseExists(parentId);
        CourseDO parentCourse = courseDataService.getById(parentId);

        CourseDO subCourse = new CourseDO();
        subCourse.setName(name);
        subCourse.setDescription(description);
        subCourse.setCreatorId(userId);
        subCourse.setParentCourseId(parentId);
        subCourse.setState(CourseState.SUBMITTED.value());
        subCourse.setRootNodeId(0L);
        subCourse.setMainCategory(parentCourse.getMainCategory());
        subCourse.setSubCategory(parentCourse.getSubCategory());

        courseDataService.insert(subCourse);

        NodeDO nodeDO = NodeDO.createRoot(userId, subCourse.getId());
        nodeDataService.insert(nodeDO);

        subCourse.setRootNodeId(nodeDO.getId());
        courseDataService.update(subCourse);
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseDTOV4> getHotCourses(int limit) {
        try {
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(limit);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);
            
            List<CourseDTOV4> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                result.add(convertToDTOWithStats(courseDO));
            }
            
            return result;
            
        } catch (Exception e) {
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
    
    // 获取热门课程完整排行榜
    public List<CourseDTOV4> getHotCoursesRanking() {
        try {
            int rankingLimit = systemProperties.getCourse().getHotCoursesRankingLimit();
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(rankingLimit);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);
            
            List<CourseDTOV4> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                result.add(convertToDTOWithStats(courseDO));
            }
            
            return result;
            
        } catch (Exception e) {
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
}
