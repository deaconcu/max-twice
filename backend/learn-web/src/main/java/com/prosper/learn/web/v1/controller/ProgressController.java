package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.response.CourseProgressResponseDTO;
import com.prosper.learn.application.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.application.dto.response.RoadmapProgressResponseDTO;
import com.prosper.learn.application.dto.response.userlearning.UserLearningDTO;
import com.prosper.learn.application.service.LearningProgressService;
import com.prosper.learn.application.service.UserLearningService;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import com.prosper.learn.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
public class ProgressController {

    private final LearningProgressService learningProgressService;
    private final UserLearningService userLearningService;

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<NodeProgressResponseDTO> markNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @JsonParam("rootNodeId") @NotNull(message = "根节点ID不能为空")
            @Positive(message = "根节点ID必须大于0")
            Long rootNodeId,
            @CurrentUser UserDO currentUser) {
        LocalDate userToday = TimeZoneUtil.getUserToday(currentUser.getTimezone());
        NodeProgressResponseDTO result = learningProgressService.markNodeCompletedWithResponse(currentUser.getId(), nodeId, rootNodeId, userToday);
        return ApiResponse.success(result);
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<NodeProgressResponseDTO> unmarkNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @JsonParam("rootNodeId") @NotNull(message = "根节点ID不能为空")
            @Positive(message = "根节点ID必须大于0")
            Long rootNodeId,
            @CurrentUser UserDO currentUser) {
        LocalDate userToday = TimeZoneUtil.getUserToday(currentUser.getTimezone());
        NodeProgressResponseDTO result = learningProgressService.unmarkNodeCompletedWithResponse(currentUser.getId(), nodeId, rootNodeId, userToday);
        return ApiResponse.success(result);
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<NodeProgressResponseDTO> getNodeCompletionStatus(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @CurrentUser UserDO currentUser) {
        NodeProgressResponseDTO result = learningProgressService.getNodeCompletionStatusResponse(currentUser.getId(), nodeId);
        return ApiResponse.success(result);
    }

    /**
     * 注册学习课程
     * 映射: POST /user/course → POST /api/v1/progress/courses/{courseId}/enrollment
     */
    @PostMapping("/progress/courses/{courseId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CourseProgressResponseDTO> startCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        userLearningService.startLearningCourse(currentUser.getId(), courseId);

        CourseProgressResponseDTO response = CourseProgressResponseDTO.builder()
                .courseId(courseId)
                .learning(true)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 取消注册学习课程
     * 映射: DELETE /api/v1/progress/courses/{courseId}/enrollment
     */
    @DeleteMapping("/progress/courses/{courseId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CourseProgressResponseDTO> cancelCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        userLearningService.cancelLearningCourse(currentUser.getId(), courseId);

        CourseProgressResponseDTO response = CourseProgressResponseDTO.builder()
                .courseId(courseId)
                .learning(false)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 获取课程进度
     * 映射: GET /user/course → GET /api/v1/progress/courses/{courseId}
     */
    @GetMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserLearningDTO<Object>> getCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        UserLearningDTO<Object> progress = userLearningService.getCourseWithObject(currentUser.getId(), courseId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有课程进度
     * 映射: GET /user/course/list → GET /api/v1/progress/courses?state=learning&lastId=123
     *
     * @param state 状态过滤（learning=进行中, completed=已完成，不传=全部）
     * @param lastId 分页游标
     */
    @GetMapping("/progress/courses")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<UserLearningDTO<Object>>> getAllCoursesProgress(
            @RequestParam(required = false) String state,
            @RequestParam(required = false)
            @Min(value = 1, message = "lastId必须大于0")
            Long lastId,
            @CurrentUser UserDO currentUser) {
        Enums.UserProgressState progressState = Enums.UserProgressState.fromName(state);
        Byte stateValue = progressState != null ? progressState.value() : null;
        List<UserLearningDTO<Object>> progressList = userLearningService.getAllCoursesProgress(
            currentUser.getId(), stateValue, lastId, 20
        );
        return ApiResponse.success(progressList);
    }

    /**
     * 更新课程进度
     * 映射: PUT /user/course → PUT /api/v1/progress/courses/{courseId}
     */
    @PutMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserLearningDTO<Object>> updateCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0")
            @Max(value = 100, message = "进度百分比不能大于100")
            Integer progressPercent,
            @CurrentUser UserDO currentUser) {

        // 转换为万分位并更新
        userLearningService.updateCourseProgress(currentUser.getId(), courseId, progressPercent * 100);

        // 返回最新的学习记录
        UserLearningDTO<Object> progress = userLearningService.getCourseWithObject(currentUser.getId(), courseId);
        return ApiResponse.success(progress);
    }

    /**
     * 删除课程进度
     * 映射: DELETE /user/course → DELETE /api/v1/progress/courses/{courseId}
     */
    @DeleteMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CourseProgressResponseDTO> deleteCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {
        // 删除课程进度就是取消学习课程
        userLearningService.cancelLearningCourse(currentUser.getId(), courseId);

        CourseProgressResponseDTO response = CourseProgressResponseDTO.builder()
                .courseId(courseId)
                .learning(false)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 注册学习路线图
     * 映射: POST /user/roadmap → POST /api/v1/progress/roadmaps/{roadmapId}/enrollment
     */
    @PostMapping("/progress/roadmaps/{roadmapId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RoadmapProgressResponseDTO> startRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        userLearningService.startLearning(currentUser.getId(), Enums.ContentType.roadmap, roadmapId);

        RoadmapProgressResponseDTO response = RoadmapProgressResponseDTO.builder()
                .roadmapId(roadmapId)
                .learning(true)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 取消注册学习路线图
     * 映射: DELETE /api/v1/progress/roadmaps/{roadmapId}/enrollment
     */
    @DeleteMapping("/progress/roadmaps/{roadmapId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RoadmapProgressResponseDTO> cancelRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        userLearningService.cancelLearning(currentUser.getId(), Enums.ContentType.roadmap, roadmapId);

        RoadmapProgressResponseDTO response = RoadmapProgressResponseDTO.builder()
                .roadmapId(roadmapId)
                .learning(false)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * 获取路线图进度
     * 映射: GET /user/roadmap → GET /api/v1/progress/roadmaps/{roadmapId}
     */
    @GetMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserLearningDTO<Object>> getRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @CurrentUser UserDO currentUser) {
        UserLearningDTO<Object> progress = userLearningService.getRoadmapWithObject(currentUser.getId(), roadmapId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有路线图进度
     * 映射: GET /user/roadmap/list → GET /api/v1/progress/roadmaps?state=learning&lastId=123
     *
     * @param state 状态过滤（learning=进行中, completed=已完成，不传=全部）
     * @param lastId 分页游标
     */
    @GetMapping("/progress/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<UserLearningDTO<Object>>> getAllRoadmapsProgress(
            @RequestParam(required = false) String state,
            @RequestParam(required = false)
            @Min(value = 1, message = "lastId必须大于0")
            Long lastId,
            @CurrentUser UserDO currentUser) {
        Enums.UserProgressState progressState = Enums.UserProgressState.fromName(state);
        Byte stateValue = progressState != null ? progressState.value() : null;
        List<UserLearningDTO<Object>> progressList = userLearningService.getByUserWithObjects(
            currentUser.getId(), Enums.ContentType.roadmap, stateValue, lastId, 20
        );
        return ApiResponse.success(progressList);
    }

    /**
     * 更新路线图进度
     * 映射: PUT /user/roadmap → PUT /api/v1/progress/roadmaps/{roadmapId}
     */
    @PutMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UserLearningDTO<Object>> updateRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空")
            @Positive(message = "路线图ID必须大于0")
            Long roadmapId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0")
            @Max(value = 100, message = "进度百分比不能大于100")
            Integer progressPercent,
            @CurrentUser UserDO currentUser) {

        // 转换为万分位
        userLearningService.updateProgress(currentUser.getId(), Enums.ContentType.roadmap, roadmapId, progressPercent * 100);
        UserLearningDTO<Object> progress = userLearningService.getRoadmapWithObject(currentUser.getId(), roadmapId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取用户正在学习的角色路线图（最多20条）
     * GET /api/v1/progress/roles/{roleId}/roadmaps/learning
     */
    @GetMapping("/progress/roles/{roleId}/roadmaps/learning")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<UserLearningDTO<Object>>> getLearningRoadmapsByRole(
            @PathVariable @NotNull(message = "角色ID不能为空")
            @Positive(message = "角色ID必须大于0")
            Long roleId,
            @RequestParam(required = false)
            @Min(value = 1, message = "lastId必须大于0")
            Long lastId,
            @CurrentUser UserDO currentUser) {

        List<UserLearningDTO<Object>> roadmaps = userLearningService.getRoadmapListByUserWithParent(
            currentUser.getId(), roleId, null, lastId, 20
        );
        return ApiResponse.success(roadmaps);
    }
}