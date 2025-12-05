package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.response.CourseCompletionResponseDTO;
import com.prosper.learn.application.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.application.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.application.dto.response.usercourse.UserCourseWithCourseDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapWithDetailDTO;
import com.prosper.learn.application.service.LearningProgressService;
import com.prosper.learn.application.service.UserCourseService;
import com.prosper.learn.application.service.UserRoadmapService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import com.prosper.learn.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 学习进度接口
 * 合并了UserCourseClient和UserClient中的进度相关功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class ProgressController {

    private final LearningProgressService learningProgressService;
    private final UserCourseService userCourseService;
    private final UserRoadmapService userRoadmapService;

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    public ApiResponse<NodeProgressResponseDTO> markNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @JsonParam("courseId") @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        NodeProgressResponseDTO result = learningProgressService.markNodeCompletedWithResponse(currentUser.getId(), nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    public ApiResponse<NodeProgressResponseDTO> unmarkNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @JsonParam("courseId") @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        NodeProgressResponseDTO result = learningProgressService.unmarkNodeCompletedWithResponse(currentUser.getId(), nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    @SaCheckLogin
    public ApiResponse<NodeWithProgressDTO> getNodeCompletionStatus(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @CurrentUser UserDO currentUser) {
        NodeWithProgressDTO result = learningProgressService.getNodeCompletionStatusResponse(currentUser.getId(), nodeId);
        return ApiResponse.success(result);
    }

    /**
     * 开始学习课程
     * 映射: POST /user/course → POST /api/v1/progress/courses/{courseId}/start
     */
    @PostMapping("/progress/courses/{courseId}/start")
    @SaCheckLogin
    public ApiResponse<Boolean> startCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        boolean result = userCourseService.startCourse(currentUser.getId(), courseId);
        return ApiResponse.success(result);
    }

    /**
     * 获取课程进度
     * 映射: GET /user/course → GET /api/v1/progress/courses/{courseId}
     */
    @GetMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    public ApiResponse<UserCourseWithCourseDTO> getCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        UserCourseWithCourseDTO progress = userCourseService.getUserCourse(currentUser.getId(), courseId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有课程进度
     * 映射: GET /user/course/list → GET /api/v1/progress/courses?lastId=123
     */
    @GetMapping("/progress/courses")
    @SaCheckLogin
    public ApiResponse<List<UserCourseWithCourseDTO>> getAllCoursesProgress(
            @RequestParam(required = false, defaultValue = "0")
            @Min(value = 0, message = "最后ID不能小于0")
            Long lastId,
            @CurrentUser UserDO currentUser) {
        List<UserCourseWithCourseDTO> progressList = userCourseService.getUserCourseList(currentUser.getId(), lastId);
        return ApiResponse.success(progressList);
    }

    /**
     * 更新课程进度
     * 映射: PUT /user/course → PUT /api/v1/progress/courses/{courseId}
     */
    @PutMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    public ApiResponse<UserCourseWithCourseDTO> updateCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0")
            @Max(value = 100, message = "进度百分比不能大于100")
            Integer progressPercent,
            @CurrentUser UserDO currentUser) {

        UserCourseWithCourseDTO progress = userCourseService.update(currentUser.getId(), courseId, progressPercent);
        return ApiResponse.success(progress);
    }

    /**
     * 删除课程进度
     * 映射: DELETE /user/course → DELETE /api/v1/progress/courses/{courseId}
     */
    @DeleteMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    public ApiResponse<String> deleteCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        userCourseService.delete(currentUser.getId(), courseId);
        return ApiResponse.success("删除成功");
    }

    /**
     * 标记课程完成
     * 映射: POST /user/complete/course/{courseId} → POST /api/v1/progress/courses/{courseId}/complete
     */
    @PostMapping("/progress/courses/{courseId}/complete")
    @SaCheckLogin
    public ApiResponse<CourseCompletionResponseDTO> markCourseCompleted(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        CourseCompletionResponseDTO result = learningProgressService.markCourseCompletedWithResponse(currentUser.getId(), courseId);
        return ApiResponse.success(result);
    }

    // =================== 路线图进度相关接口 ===================

    /**
     * 开始学习路线图
     * 映射: POST /user/roadmap → POST /api/v1/progress/roadmaps/{roadmapId}/start
     */
    @PostMapping("/progress/roadmaps/{roadmapId}/start")
    @SaCheckLogin
    public ApiResponse<Boolean> startRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        boolean result = userRoadmapService.startRoadmap(currentUser.getId(), roadmapId);
        return ApiResponse.success(result);
    }

    /**
     * 获取路线图进度
     * 映射: GET /user/roadmap → GET /api/v1/progress/roadmaps/{roadmapId}
     */
    @GetMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    public ApiResponse<UserRoadmapWithDetailDTO> getRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        UserRoadmapWithDetailDTO progress = userRoadmapService.getUserRoadmap(currentUser.getId(), roadmapId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有路线图进度
     * 映射: GET /user/roadmap/list → GET /api/v1/progress/roadmaps
     */
    @GetMapping("/progress/roadmaps")
    @SaCheckLogin
    public ApiResponse<List<UserRoadmapWithDetailDTO>> getAllRoadmapsProgress(@CurrentUser UserDO currentUser) {
        List<UserRoadmapWithDetailDTO> progressList = userRoadmapService.getUserAllRoadmap(currentUser.getId());
        return ApiResponse.success(progressList);
    }

    /**
     * 更新路线图进度
     * 映射: PUT /user/roadmap → PUT /api/v1/progress/roadmaps/{roadmapId}
     */
    @PutMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    public ApiResponse<UserRoadmapSummaryDTO> updateRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0")
            @Max(value = 100, message = "进度百分比不能大于100")
            Integer progressPercent,
            @CurrentUser UserDO currentUser) {

        UserRoadmapSummaryDTO progress = userRoadmapService.updateProgress(currentUser.getId(), roadmapId, progressPercent);
        return ApiResponse.success(progress);
    }

    /**
     * 删除路线图进度
     * 映射: DELETE /user/roadmap → DELETE /api/v1/progress/roadmaps/{roadmapId}
     */
    @DeleteMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    public ApiResponse<String> deleteRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        userRoadmapService.deleteRoadmap(currentUser.getId(), roadmapId);
        return ApiResponse.success("删除成功");
    }
}