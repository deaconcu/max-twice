package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.LearningProgressService;
import com.prosper.learn.domain.service.business.UserCourseService;
import com.prosper.learn.domain.service.business.UserRoadmapService;
import com.prosper.learn.dto.response.UserCourseDTO;
import com.prosper.learn.dto.response.UserRoadmapDTO;
import com.prosper.learn.dto.response.NodeProgressResponseDTO;
import com.prosper.learn.dto.response.CourseCompletionResponseDTO;
import com.prosper.learn.dto.response.NodeDTOV2;
import lombok.RequiredArgsConstructor;
import com.prosper.learn.api.v1.annotation.JsonParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习进度接口
 * 合并了UserCourseClient和UserClient中的进度相关功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProgressController {

    private final LearningProgressService learningProgressService;
    private final UserCourseService userCourseService;
    private final UserRoadmapService userRoadmapService;

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<NodeProgressResponseDTO> markNodeCompleted(
            @PathVariable Long nodeId, 
            @JsonParam("courseId") Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        NodeProgressResponseDTO result = learningProgressService.markNodeCompletedWithResponse(userId, nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<NodeProgressResponseDTO> unmarkNodeCompleted(
            @PathVariable Long nodeId, 
            @JsonParam("courseId") Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        NodeProgressResponseDTO result = learningProgressService.unmarkNodeCompletedWithResponse(userId, nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    public ApiResponse<NodeDTOV2> getNodeCompletionStatus(@PathVariable Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        NodeDTOV2 result = learningProgressService.getNodeCompletionStatusResponse(userId, nodeId);
        return ApiResponse.success(result);
    }

    /**
     * 开始学习课程
     * 映射: POST /user/course → POST /api/v1/progress/courses/{courseId}/start
     */
    @PostMapping("/progress/courses/{courseId}/start")
    public ApiResponse<Boolean> startCourse(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = userCourseService.startCourse(userId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 获取课程进度
     * 映射: GET /user/course → GET /api/v1/progress/courses/{courseId}
     */
    @GetMapping("/progress/courses/{courseId}")
    public ApiResponse<UserCourseDTO> getCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCourseDTO progress = userCourseService.getUserCourse(userId, courseId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有课程进度
     * 映射: GET /user/course/list → GET /api/v1/progress/courses?lastId=123
     */
    @GetMapping("/progress/courses")
    public ApiResponse<List<UserCourseDTO>> getAllCoursesProgress(@RequestParam(required = false, defaultValue = "0") Long lastId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserCourseDTO> progressList = userCourseService.getUserCourseList(userId, lastId);
        return ApiResponse.success(progressList);
    }

    /**
     * 更新课程进度
     * 映射: PUT /user/course → PUT /api/v1/progress/courses/{courseId}
     */
    @PutMapping("/progress/courses/{courseId}")
    public ApiResponse<UserCourseDTO> updateCourseProgress(
            @PathVariable Long courseId, 
            @JsonParam("progressPercent") Integer progressPercent) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        UserCourseDTO progress = userCourseService.update(userId, courseId, progressPercent);
        return ApiResponse.success(progress);
    }

    /**
     * 删除课程进度
     * 映射: DELETE /user/course → DELETE /api/v1/progress/courses/{courseId}
     */
    @DeleteMapping("/progress/courses/{courseId}")
    public ApiResponse<String> deleteCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userCourseService.delete(userId, courseId);
        return ApiResponse.success("删除成功");
    }

    /**
     * 标记课程完成
     * 映射: POST /user/complete/course/{courseId} → POST /api/v1/progress/courses/{courseId}/complete
     */
    @PostMapping("/progress/courses/{courseId}/complete")
    public ApiResponse<CourseCompletionResponseDTO> markCourseCompleted(@PathVariable Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        CourseCompletionResponseDTO result = learningProgressService.markCourseCompletedWithResponse(userId, courseId);
        return ApiResponse.success(result);
    }

    // =================== 路线图进度相关接口 ===================

    /**
     * 开始学习路线图
     * 映射: POST /user/roadmap → POST /api/v1/progress/roadmaps/{roadmapId}/start
     */
    @PostMapping("/progress/roadmaps/{roadmapId}/start")
    public ApiResponse<Boolean> startRoadmap(@PathVariable Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = userRoadmapService.startRoadmap(userId, roadmapId);
        return ApiResponse.success(result);
    }

    /**
     * 获取路线图进度
     * 映射: GET /user/roadmap → GET /api/v1/progress/roadmaps/{roadmapId}
     */
    @GetMapping("/progress/roadmaps/{roadmapId}")
    public ApiResponse<UserRoadmapDTO> getRoadmapProgress(@PathVariable Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserRoadmapDTO progress = userRoadmapService.getUserRoadmap(userId, roadmapId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有路线图进度
     * 映射: GET /user/roadmap/list → GET /api/v1/progress/roadmaps
     */
    @GetMapping("/progress/roadmaps")
    public ApiResponse<List<UserRoadmapDTO>> getAllRoadmapsProgress() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserRoadmapDTO> progressList = userRoadmapService.getUserAllRoadmap(userId);
        return ApiResponse.success(progressList);
    }

    /**
     * 更新路线图进度
     * 映射: PUT /user/roadmap → PUT /api/v1/progress/roadmaps/{roadmapId}
     */
    @PutMapping("/progress/roadmaps/{roadmapId}")
    public ApiResponse<UserRoadmapDTO> updateRoadmapProgress(
            @PathVariable Long roadmapId, 
            @JsonParam("progressPercent") Integer progressPercent) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        UserRoadmapDTO progress = userRoadmapService.updateProgress(userId, roadmapId, progressPercent);
        return ApiResponse.success(progress);
    }

    /**
     * 删除路线图进度
     * 映射: DELETE /user/roadmap → DELETE /api/v1/progress/roadmaps/{roadmapId}
     */
    @DeleteMapping("/progress/roadmaps/{roadmapId}")
    public ApiResponse<String> deleteRoadmapProgress(@PathVariable Long roadmapId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userRoadmapService.deleteRoadmap(userId, roadmapId);
        return ApiResponse.success("删除成功");
    }
}