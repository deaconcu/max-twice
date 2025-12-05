package com.prosper.learn.content.course;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 课程领域服务
 *
 * 职责：
 * - 课程领域的业务规则验证
 * - 课程的核心业务逻辑（CRUD）
 * - 课程状态管理
 *
 * 不包含：
 * - 跨域调用（如发送消息通知）
 * - DTO 转换（由 ApplicationService 负责）
 */
@Service
@RequiredArgsConstructor
public class CourseDomainService {

    private final CourseDataService courseDataService;
    private final SystemProperties systemProperties;

    // ========== 验证方法 ==========

    /**
     * 验证课程是否存在并返回
     */
    public CourseDO validateAndGet(long courseId) {
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
        }
        return courseDO;
    }

    /**
     * 验证父课程是否存在
     */
    public void validateParentExists(long parentId) {
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
     * 验证课程状态是否可以审核通过
     */
    public void validateStateForApproval(CourseDO courseDO) {
        if (!systemProperties.getCourse().isEnableStateValidation()) {
            return;
        }
        if (ContentState.PUBLISHED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_APPROVED.exception();
        }
    }

    /**
     * 验证课程状态是否可以拒绝
     */
    public void validateStateForRejection(CourseDO courseDO) {
        if (!systemProperties.getCourse().isEnableStateValidation()) {
            return;
        }
        if (ContentState.REJECTED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_BANNED.exception();
        }
        if (ContentState.BANNED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_BANNED.exception();
        }
    }

    /**
     * 验证课程是否可以被封禁
     */
    public void validateStateForBan(CourseDO courseDO) {
        if (!systemProperties.getCourse().isEnableStateValidation()) {
            return;
        }
        if (ContentState.BANNED.value().equals(courseDO.getState())) {
            throw ErrorCode.COURSE_ALREADY_BANNED.exception();
        }
    }

    /**
     * 验证数据库操作结果
     */
    public void validateOperationResult(int rowsAffected) {
        if (rowsAffected == 0) {
            throw ErrorCode.COURSE_STATE_CONFLICT.exception();
        }
    }

    // ========== 查询方法 ==========

    /**
     * 根据ID获取课程
     */
    public CourseDO getById(long id) {
        return courseDataService.getById(id);
    }

    /**
     * 检查课程是否存在
     */
    public boolean exists(long id) {
        CourseDO courseDO = courseDataService.getById(id);
        return courseDO != null;
    }

    /**
     * 批量获取课程，返回 Map
     */
    public Map<Long, CourseDO> getByIds(List<Long> ids) {
        Map<Long, CourseDO> courseMap = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return courseMap;
        }

        List<CourseDO> courseList = courseDataService.getByIds(ids);
        return courseList.stream()
                .collect(Collectors.toMap(CourseDO::getId, course -> course));
    }

    /**
     * 检查用户是否是课程创建者
     */
    public boolean isCreator(Long courseId, Long userId) {
        CourseDO course = validateAndGet(courseId);
        return course.getCreatorId().equals(userId);
    }

    // ========== 业务方法 ==========

    /**
     * 更新课程基本信息
     *
     * 注意：权限验证应该由调用方（ApplicationService）完成
     */
    @Transactional
    public void updateCourse(Long courseId, String name, String description,
                            Integer mainCategory, Integer subCategory) {
        CourseDO courseDO = validateAndGet(courseId);

        courseDO.setName(name);
        courseDO.setDescription(description);
        courseDO.setMainCategory(mainCategory);
        courseDO.setSubCategory(subCategory);

        courseDataService.update(courseDO);
    }

    /**
     * 删除课程
     *
     * 注意：权限验证应该由调用方完成
     */
    @Transactional
    public void deleteCourse(long courseId) {
        validateAndGet(courseId);

        int rowsAffected = courseDataService.delete(courseId);
        if (rowsAffected == 0) {
            throw ErrorCode.COURSE_DELETE_FAILED.exception();
        }
    }
}
