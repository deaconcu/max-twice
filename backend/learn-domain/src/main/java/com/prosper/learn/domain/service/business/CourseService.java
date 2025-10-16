package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.Enums.CourseState;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.domain.util.converter.CourseConverter;
import com.prosper.learn.dto.request.CreateCourseRequest;
import com.prosper.learn.dto.request.UpdateCourseRequest;
import com.prosper.learn.dto.response.CourseDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.NodeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


    // ========== toDTO ==========

    /**
     * dtov1 = id + name + description + mainCategory + subCategory
     */
    public CourseDTO toDTOV2(CourseDO courseDO) {
        return courseConverter.toDTOV2(courseDO);
    }

    public List<CourseDTO> toDTOV2(List<CourseDO> courseDOList) {
        return courseConverter.toDTOV2(courseDOList);
    }

    /**
     * dtoV3 = id + name
     */
    public CourseDTO toDTOV3(CourseDO courseDO) {
        return courseConverter.toDTOV3(courseDO);
    }

    public List<CourseDTO> toDTOV3(List<CourseDO> courseDOList) {
        return courseConverter.toDTOV3(courseDOList);
    }

    /**
     * dtoV4 = dto + parentCourse(dtoV3)
     */
    public CourseDTO toDTOV4(CourseDO courseDO) {
        if (courseDO == null) return null;
        
        CourseDTO dto = courseConverter.toDTO(courseDO);
        
        // 填充父课程信息
        if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            if (parentCourseDO != null) {
                CourseDTO parentDTO = courseConverter.toDTOV3(parentCourseDO);
                dto.setParentCourse(parentDTO);
            }
        }
        return dto;
    }
    
    /**
     * dtov5 = dtov4 + subcribed + progress
     */
    public CourseDTO toDTOV5(CourseDO courseDO, boolean subscribed, int progress) {
        if (courseDO == null) return null;

        CourseDTO dto = toDTOV4(courseDO);
        dto.setSubscribed(subscribed);
        dto.setProgress(progress);
        return dto;
    }

    /**
     * dtov6 = dto + learnerCount + subscriptionCount
     */
    private CourseDTO toDTOV6(CourseDO courseDO) {
        //CourseDTOV4 courseDTO = Converter.INSTANCE.toCourseDTOWithParent(courseDO, courseDataService);
        CourseDTO courseDTO = courseConverter.toDTO(courseDO);

        try {
            CourseRankingService.CourseStats stats = courseRankingService.getCourseStats(courseDO.getId());
            courseDTO.setLearnerCount((int) stats.getLearningCount());
            courseDTO.setSubscriptionCount((int) stats.getSubscriptionCount());
        } catch (Exception e) {
            // 统计信息获取失败时设置默认值
            courseDTO.setLearnerCount(0);
            courseDTO.setSubscriptionCount(0);
        }
        return courseDTO;
    }

    // ========== 公共业务方法 ==========

    public CourseDTO getCourseById(Long id) {
        CourseDO course = validateCourseExists(id);
        return toDTOV4(course);
    }

    public List<CourseDTO> searchCoursesByName(String name) {
        int searchLimit = systemProperties.getCourse().getSearchLimit();
        List<CourseDO> courseList = courseDataService.searchByName(name, searchLimit);
        return toDTOV3(courseList);
    }

    public Map<Long, CourseDO> getCourseMap(List<Long> ids) {
        Map<Long, CourseDO> courseMap = new HashMap<>();
        if (ids == null || ids.isEmpty()) return courseMap;

        List<CourseDO> courseList = courseDataService.getByIds(ids);
        courseMap = courseList.stream().collect(Collectors.toMap(CourseDO::getId, course -> course));
        return courseMap;
    }

    /**
     * 获取子课程列表（仅包含已批准的子课程）, 用于展示在课程详情页面
     * @param parentCourseId
     * @return
     */
    public List<CourseDTO> getSubCourses(long parentCourseId) {
        return toDTOV2(courseDataService.listByParentAndState(CourseState.APPROVED, parentCourseId));
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

    public CourseDTO getById(long id, Enums.DTOVersion version) {
        CourseDO courseDO = courseDataService.getById(id);
        if (courseDO == null) return null;

        switch (version) {
            case V2 -> toDTOV2(courseDO);
            case V4 -> toDTOV4(courseDO);
        }
        return null;
    }

    public CourseDTO getCourseDTOV4ById(Long courseId) {
        if (courseId == null) return null;
        CourseDO courseDO = courseDataService.getById(courseId);
        return courseDO != null ? toDTOV4(courseDO) : null;
    }

    // 新增：根据状态和lastId获取课程列表
    public List<CourseDTO> getListByStateAndLastId(CourseState state, long lastId) {
        List<CourseDO> courseDOList = courseDataService.listByStateAndLastId(state, lastId);
        return courseDOList.stream()
                .map(this::toDTOV4)
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据主分类和子分类获取已批准的课程列表
    public List<CourseDTO> getListByCategory(int mainCategory, int subCategory) {
        List<CourseDO> courseDOList;
        courseDOList = courseDataService.listRootByCategory(mainCategory, subCategory);
        return courseDOList.stream()
                .map(this::toDTOV4)
                .collect(java.util.stream.Collectors.toList());
    }

    // 新增：根据父课程ID获取子课程列表
    public List<CourseDTO> getListByParent(long parentId, CourseState state) {
        List<CourseDO> courseDOList;
        if (state == null) { // null表示获取所有状态
            courseDOList = courseDataService.listByParent(parentId);
        } else {
            courseDOList = courseDataService.listByParentAndState(state, parentId);
        }
        return courseDOList.stream()
                .map(this::toDTOV4)
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

        // 验证通过后创建对象
        CourseDO course = new CourseDO();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setCreatorId(userId);
        course.setRootNodeId(0L);
        course.setParentCourseId(0L);
        course.setState(CourseState.SUBMITTED.value());
        course.setMainCategory(request.getMainCategory());
        course.setSubCategory(request.getSubCategory());
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
        subCourse.setRootNodeId(0L);
        subCourse.setParentCourseId(parentId);
        subCourse.setState(CourseState.SUBMITTED.value());
        subCourse.setMainCategory(parentCourse.getMainCategory());
        subCourse.setSubCategory(parentCourse.getSubCategory());

        courseDataService.insert(subCourse);

        NodeDO nodeDO = NodeDO.createRoot(userId, subCourse.getId());
        nodeDataService.insert(nodeDO);

        subCourse.setRootNodeId(nodeDO.getId());
        courseDataService.update(subCourse);
    }

    // 获取热门课程（使用Redis排行榜）
    public List<CourseDTO> getHotCourses(int limit) {
        try {
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(limit);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);
            
            List<CourseDTO> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                result.add(toDTOV6(courseDO));
            }
            
            return result;
            
        } catch (Exception e) {
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
    
    // 获取热门课程完整排行榜
    public List<CourseDTO> getHotCoursesRanking() {
        try {
            int rankingLimit = systemProperties.getCourse().getHotCoursesRankingLimit();
            List<Long> hotCourseIds = courseRankingService.getHotCourseIds(rankingLimit);
            
            if (hotCourseIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<CourseDO> courseDOList = courseDataService.getByIds(hotCourseIds);
            
            List<CourseDTO> result = new ArrayList<>();
            for (CourseDO courseDO : courseDOList) {
                result.add(toDTOV6(courseDO));
            }
            
            return result;
            
        } catch (Exception e) {
            throw ErrorCode.COURSE_OPERATION_FAILED.exception(e);
        }
    }
}
