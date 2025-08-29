package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.LearningProgressService;
import com.prosper.learn.domain.service.business.UserCourseService;
import com.prosper.learn.dto.UserCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<Map<String, Object>> markNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = learningProgressService.markNodeCompletedWithResponse(userId, nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    public ApiResponse<Map<String, Object>> unmarkNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = learningProgressService.unmarkNodeCompletedWithResponse(userId, nodeId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    public ApiResponse<Map<String, Object>> getNodeCompletionStatus(@PathVariable Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = learningProgressService.getNodeCompletionStatusResponse(userId, nodeId);
        return ApiResponse.success(result);
    }

    /**
     * 开始学习课程
     * 映射: POST /user/course → POST /api/v1/progress/courses/{courseId}/start
     */
    @PostMapping("/progress/courses/{courseId}/start")
    public ApiResponse<Boolean> startCourse(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean result = userCourseService.startCourseWithValidation(userId, courseId);
        return ApiResponse.success(result);
    }

    /**
     * 获取课程进度
     * 映射: GET /user/course → GET /api/v1/progress/courses/{courseId}
     */
    @GetMapping("/progress/courses/{courseId}")
    public ApiResponse<UserCourseDTO> getCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCourseDTO progress = userCourseService.getUserCourseWithValidation(userId, courseId);
        return ApiResponse.success(progress);
    }

    /**
     * 获取所有课程进度
     * 映射: GET /user/course/list → GET /api/v1/progress/courses?lastId=123
     */
    @GetMapping("/progress/courses")
    public ApiResponse<List<UserCourseDTO>> getAllCoursesProgress(@RequestParam(required = false, defaultValue = "0") Long lastId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserCourseDTO> progressList = userCourseService.getUserCourseListWithValidation(userId, lastId);
        return ApiResponse.success(progressList);
    }

    /**
     * 更新课程进度
     * 映射: PUT /user/course → PUT /api/v1/progress/courses/{courseId}
     */
    @PutMapping("/progress/courses/{courseId}")
    public ApiResponse<UserCourseDTO> updateCourseProgress(
            @PathVariable Long courseId, 
            @RequestParam Integer progressPercent) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        UserCourseDTO progress = userCourseService.updateWithValidation(userId, courseId, progressPercent);
        return ApiResponse.success(progress);
    }

    /**
     * 删除课程进度
     * 映射: DELETE /user/course → DELETE /api/v1/progress/courses/{courseId}
     */
    @DeleteMapping("/progress/courses/{courseId}")
    public ApiResponse<String> deleteCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();
        userCourseService.deleteWithValidation(userId, courseId);
        return ApiResponse.success("删除成功");
    }

    /**
     * 标记课程完成
     * 映射: POST /user/complete/course/{courseId} → POST /api/v1/progress/courses/{courseId}/complete
     */
    @PostMapping("/progress/courses/{courseId}/complete")
    public ApiResponse<Map<String, Object>> markCourseCompleted(@PathVariable Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = learningProgressService.markCourseCompletedWithResponse(userId, courseId);
        return ApiResponse.success(result);
    }
}