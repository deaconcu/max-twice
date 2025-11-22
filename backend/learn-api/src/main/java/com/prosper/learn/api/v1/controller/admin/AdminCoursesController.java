package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.CourseService;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.request.UpdateCourseRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.course.*;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminCoursesController {

    private final CourseService courseService;

    /**
     * 管理后台：按状态获取课程列表
     * 映射: GET /api/v1/admin/courses?state=0&lastId=123
     */
    @GetMapping("/courses")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> getAdminCourses(
            @RequestParam(required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory) {

        // 按状态查询
        if (state != null) {
            ContentState courseState = ContentState.getByValue(state.intValue());
            if (courseState == null) {
                throw ErrorCode.INVALID_PARAMETER.exception("Invalid course state: " + state);
            }
            List<CourseDetailDTO> courseList = courseService.getListByStateAndLastId(courseState, lastId);
            return ApiResponse.success(courseList);
        }

        // 按分类查询（管理员版本）
        if (mainCategory != null && subCategory != null) {
            // TODO: 当前复用普通接口，只返回已发布课程。如需返回其他状态，需新增 Service 方法
            List<CourseDetailDTO> courseList = courseService.getListByCategory(mainCategory, subCategory);
            return ApiResponse.success(courseList);
        }

        throw ErrorCode.INVALID_PARAMETER.exception("缺少必要参数：state 或 mainCategory+subCategory");
    }

    /**
     * 管理后台：按ID获取课程详情
     * GET /api/v1/admin/courses/{id}
     * TODO: 当前复用普通方法，可能无法获取非已发布状态的课程。需验证并可能需要新增 Service 方法
     */
    @GetMapping("/courses/{id}")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<CourseDetailDTO> getAdminCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long id) {
        CourseDetailDTO course = courseService.getCourseById(id);
        return ApiResponse.success(course);
    }

    /**
     * 管理后台：查询子课程列表
     * GET /api/v1/admin/courses/{parentId}/subcourses?state=0
     * TODO: 当前复用普通方法，可能有状态过滤。需验证并可能需要新增 Service 方法
     */
    @GetMapping("/courses/{parentId}/subcourses")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<List<CourseDetailDTO>> getAdminSubcourses(
            @PathVariable @NotNull(message = "父课程ID不能为空")
            @Positive(message = "父课程ID必须大于0") Long parentId,
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state) {

        ContentState courseState = state != null ? ContentState.getByValue(state) : null;
        List<CourseDetailDTO> courseList = courseService.getListByParent(parentId, courseState);
        return ApiResponse.success(courseList);
    }

    /**
     * 管理后台：课程审核操作
     * 映射: POST /course/operate → POST /api/v1/admin/courses/{id}/approve
     */
    @PostMapping("/courses/{id}/approve")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "#request.action == 'APPROVE' ? '审核通过课程' : (#request.action == 'REJECT' ? '审核拒绝课程' : (#request.action == 'BAN' ? '屏蔽课程' : (#request.action == 'DELETE' ? '删除课程' : '恢复课程')))",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ApiResponse<ApprovalResponseDTO> approveCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                courseService.approve(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("course")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                courseService.reject(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("course")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                courseService.ban(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(id)
                        .objectType("course")
                        .action("ban")
                        .build();
            }
            case "delete" -> {
                courseService.delete(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("删除成功")
                        .objectId(id)
                        .objectType("course")
                        .action("delete")
                        .build();
            }
            case "restore" -> {
                // 恢复课程到待审核状态
                courseService.approve(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("恢复成功")
                        .objectId(id)
                        .objectType("course")
                        .action("restore")
                        .build();
            }
            default -> throw ErrorCode.INVALID_PARAMETER.exception("不支持的操作类型: " + request.getAction());
        };

        return ApiResponse.success(response);
    }

    /**
     * 管理后台：更新课程信息
     * 映射: PUT /course/{id} → PUT /api/v1/admin/courses/{id}
     */
    @PutMapping("/courses/{id}")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "更新课程信息",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Course",
        targetId = "#id"
    )
    public ApiResponse<Object> updateCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.updateCourse(id, request, currentUser);
        return ApiResponse.success("更新成功");
    }
}
