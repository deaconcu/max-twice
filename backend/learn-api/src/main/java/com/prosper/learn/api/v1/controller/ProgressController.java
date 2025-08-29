package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.LearningProgressService;
import com.prosper.learn.domain.service.UserCourseService;
import com.prosper.learn.dto.UserCourseDTO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.mapper.UserCourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final UserCourseMapper userCourseMapper;

    /**
     * 标记节点完成
     * 映射: POST /user/complete/{nodeId} → POST /api/v1/progress/nodes/{nodeId}/complete
     */
    @PostMapping("/progress/nodes/{nodeId}/complete")
    public ResponseEntity<ApiResponse<Object>> markNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean isNewlyCompleted = learningProgressService.markNodeCompleted(userId, nodeId, courseId);
        
        UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", true);
        result.put("isNewlyCompleted", isNewlyCompleted);
        result.put("courseProgress", courseProgress);
        
        long totalCompleted = learningProgressService.getUserCompletedCount(userId);
        result.put("totalCompletedNodes", totalCompleted);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 取消节点完成
     * 映射: DELETE /user/complete/{nodeId} → DELETE /api/v1/progress/nodes/{nodeId}/complete
     */
    @DeleteMapping("/progress/nodes/{nodeId}/complete")
    public ResponseEntity<ApiResponse<Object>> unmarkNodeCompleted(@PathVariable Long nodeId, @RequestParam Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean wasRemoved = learningProgressService.unmarkNodeCompleted(userId, nodeId, courseId);
        
        UserCourseDO userCourse = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        Integer courseProgress = userCourse != null ? userCourse.getProgressPercent() : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", false);
        result.put("wasRemoved", wasRemoved);
        result.put("courseProgress", courseProgress);
        
        long totalCompleted = learningProgressService.getUserCompletedCount(userId);
        result.put("totalCompletedNodes", totalCompleted);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 检查节点完成状态
     * 映射: GET /user/complete/{nodeId} → GET /api/v1/progress/nodes/{nodeId}/status
     */
    @GetMapping("/progress/nodes/{nodeId}/status")
    public ResponseEntity<ApiResponse<Object>> getNodeCompletionStatus(@PathVariable Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean isCompleted = learningProgressService.isNodeCompleted(userId, nodeId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", nodeId);
        result.put("completed", isCompleted);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 开始学习课程
     * 映射: POST /user/course → POST /api/v1/progress/courses/{courseId}/start
     */
    @PostMapping("/progress/courses/{courseId}/start")
    public ResponseEntity<ApiResponse<Object>> startCourse(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        boolean result = userCourseService.startCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取课程进度
     * 映射: GET /user/course → GET /api/v1/progress/courses/{courseId}
     */
    @GetMapping("/progress/courses/{courseId}")
    public ResponseEntity<ApiResponse<UserCourseDTO>> getCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        UserCourseDTO progress = userCourseService.getUserCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 获取所有课程进度
     * 映射: GET /user/course/list → GET /api/v1/progress/courses?lastId=123
     */
    @GetMapping("/progress/courses")
    public ResponseEntity<ApiResponse<List<UserCourseDTO>>> getAllCoursesProgress(@RequestParam(required = false, defaultValue = "0") Long lastId) {
        if (lastId == null || lastId < 0) lastId = 0L;
        Long userId = StpUtil.getLoginIdAsLong();

        List<UserCourseDTO> progressList = userCourseService.getUserCourseList(userId, lastId);
        return ResponseEntity.ok(ApiResponse.success(progressList));
    }

    /**
     * 更新课程进度
     * 映射: PUT /user/course → PUT /api/v1/progress/courses/{courseId}
     */
    @PutMapping("/progress/courses/{courseId}")
    public ResponseEntity<ApiResponse<UserCourseDTO>> updateCourseProgress(
            @PathVariable Long courseId, 
            @RequestParam Integer progressPercent) {
        
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("进度百分比必须在0-100之间");
        }

        UserCourseDTO progress = userCourseService.update(userId, courseId, progressPercent);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * 删除课程进度
     * 映射: DELETE /user/course → DELETE /api/v1/progress/courses/{courseId}
     */
    @DeleteMapping("/progress/courses/{courseId}")
    public ResponseEntity<ApiResponse<Object>> deleteCourseProgress(@PathVariable Long courseId) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空");
        }

        userCourseService.delete(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    /**
     * 标记课程完成
     * 映射: POST /user/complete/course/{courseId} → POST /api/v1/progress/courses/{courseId}/complete
     */
    @PostMapping("/progress/courses/{courseId}/complete")
    public ResponseEntity<ApiResponse<Object>> markCourseCompleted(@PathVariable Long courseId) {
        long userId = StpUtil.getLoginIdAsLong();
        
        boolean result = learningProgressService.markCourseCompleted(userId, courseId);
        
        if (result) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("courseId", courseId);
            responseData.put("completed", true);
            responseData.put("message", "课程已标记为完成");
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } else {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }
    }
}