package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.response.CourseProgressResponseDTO;
import com.twicemax.application.dto.response.NodeProgressResponseDTO;
import com.twicemax.application.dto.response.RoadmapProgressResponseDTO;
import com.twicemax.application.dto.response.userlearning.UserLearningDTO;
import com.twicemax.application.service.LearningProgressService;
import com.twicemax.application.service.UserLearningService;
import com.twicemax.shared.common.util.TimeZoneUtil;
import com.twicemax.shared.domain.Enums;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.JsonParam;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 学习进度接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class ProgressController {

    private final LearningProgressService learningProgressService;
    private final UserLearningService userLearningService;

    @PostMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public NodeProgressResponseDTO markNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @JsonParam("rootNodeId") @NotNull(message = "根节点ID不能为空") @Positive(message = "根节点ID必须大于0") Long rootNodeId,
            @CurrentUser UserDO currentUser) {
        LocalDate userToday = TimeZoneUtil.getUserToday(currentUser.getTimezone());
        return learningProgressService.markNodeCompletedWithResponse(currentUser.getId(), nodeId, rootNodeId, userToday);
    }

    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public NodeProgressResponseDTO unmarkNodeCompleted(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @JsonParam("rootNodeId") @NotNull(message = "根节点ID不能为空") @Positive(message = "根节点ID必须大于0") Long rootNodeId,
            @CurrentUser UserDO currentUser) {
        LocalDate userToday = TimeZoneUtil.getUserToday(currentUser.getTimezone());
        return learningProgressService.unmarkNodeCompletedWithResponse(currentUser.getId(), nodeId, rootNodeId, userToday);
    }

    @GetMapping("/progress/nodes/{nodeId}/status")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public NodeProgressResponseDTO getNodeCompletionStatus(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @CurrentUser UserDO currentUser) {
        return learningProgressService.getNodeCompletionStatusResponse(currentUser.getId(), nodeId);
    }

    @PostMapping("/progress/courses/{courseId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CourseProgressResponseDTO startCourse(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        userLearningService.startLearningCourse(currentUser.getId(), courseId);
        return CourseProgressResponseDTO.builder().courseId(courseId).learning(true).build();
    }

    @DeleteMapping("/progress/courses/{courseId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CourseProgressResponseDTO cancelCourse(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        userLearningService.cancelLearningCourse(currentUser.getId(), courseId);
        return CourseProgressResponseDTO.builder().courseId(courseId).learning(false).build();
    }

    @GetMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserLearningDTO<Object> getCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        return userLearningService.getCourseWithObject(currentUser.getId(), courseId);
    }

    @GetMapping("/progress/courses")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<UserLearningDTO<Object>> getAllCoursesProgress(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        Enums.UserProgressState progressState = Enums.UserProgressState.fromName(state);
        Byte stateValue = progressState != null ? progressState.value() : null;
        return userLearningService.getAllCoursesProgress(currentUser.getId(), stateValue, cursor, 20);
    }

    @PutMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserLearningDTO<Object> updateCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0") @Max(value = 100, message = "进度百分比不能大于100") Integer progressPercent,
            @CurrentUser UserDO currentUser) {
        userLearningService.updateCourseProgress(currentUser.getId(), courseId, progressPercent * 100);
        return userLearningService.getCourseWithObject(currentUser.getId(), courseId);
    }

    @DeleteMapping("/progress/courses/{courseId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CourseProgressResponseDTO deleteCourseProgress(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        userLearningService.cancelLearningCourse(currentUser.getId(), courseId);
        return CourseProgressResponseDTO.builder().courseId(courseId).learning(false).build();
    }

    @PostMapping("/progress/roadmaps/{roadmapId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapProgressResponseDTO startRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @CurrentUser UserDO currentUser) {
        userLearningService.startLearning(currentUser.getId(), Enums.ContentType.roadmap, roadmapId);
        return RoadmapProgressResponseDTO.builder().roadmapId(roadmapId).learning(true).build();
    }

    @DeleteMapping("/progress/roadmaps/{roadmapId}/enrollment")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapProgressResponseDTO cancelRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @CurrentUser UserDO currentUser) {
        userLearningService.cancelLearning(currentUser.getId(), Enums.ContentType.roadmap, roadmapId);
        return RoadmapProgressResponseDTO.builder().roadmapId(roadmapId).learning(false).build();
    }

    @GetMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserLearningDTO<Object> getRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @CurrentUser UserDO currentUser) {
        return userLearningService.getRoadmapWithObject(currentUser.getId(), roadmapId);
    }

    @GetMapping("/progress/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<UserLearningDTO<Object>> getAllRoadmapsProgress(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        Enums.UserProgressState progressState = Enums.UserProgressState.fromName(state);
        Byte stateValue = progressState != null ? progressState.value() : null;
        return userLearningService.getByUserWithObjects(currentUser.getId(), Enums.ContentType.roadmap, stateValue, cursor, 20);
    }

    @PutMapping("/progress/roadmaps/{roadmapId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserLearningDTO<Object> updateRoadmapProgress(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long roadmapId,
            @JsonParam("progressPercent") @NotNull(message = "进度百分比不能为空")
            @Min(value = 0, message = "进度百分比不能小于0") @Max(value = 100, message = "进度百分比不能大于100") Integer progressPercent,
            @CurrentUser UserDO currentUser) {
        userLearningService.updateProgress(currentUser.getId(), Enums.ContentType.roadmap, roadmapId, progressPercent * 100);
        return userLearningService.getRoadmapWithObject(currentUser.getId(), roadmapId);
    }

    @GetMapping("/progress/roles/{roleId}/roadmaps/learning")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<UserLearningDTO<Object>> getLearningRoadmapsByRole(
            @PathVariable @NotNull(message = "角色ID不能为空") @Positive(message = "角色ID必须大于0") Long roleId,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return userLearningService.getRoadmapListByUserWithParent(currentUser.getId(), roleId, null, cursor, 20);
    }
}
