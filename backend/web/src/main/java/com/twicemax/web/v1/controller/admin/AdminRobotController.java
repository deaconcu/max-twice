package com.twicemax.web.v1.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.application.dto.response.RobotQueueStatsDTO;
import com.twicemax.application.dto.response.RobotRoadmapDraftDTO;
import com.twicemax.application.dto.response.RobotRoadmapTaskDTO;
import com.twicemax.application.service.CourseService;
import com.twicemax.application.service.NodeService;
import com.twicemax.application.service.robot.RobotQueueService;
import com.twicemax.application.service.robot.RobotScanner;
import com.twicemax.application.service.robot.RoadmapGenerationService;
import com.twicemax.infrastructure.ai.AIService;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.application.dto.ApiResponse;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * Robot 内容生成管理接口
 */
@RestController
@RequestMapping("/v1/admin/robot")
@RequiredArgsConstructor
@Validated
public class AdminRobotController {

    private final RobotScanner scanner;
    private final RobotQueueService robotQueueService;
    private final AIService aiService;
    private final RoadmapGenerationService roadmapGenerationService;
    private final SystemProperties systemProperties;
    private final CourseService courseService;
    private final NodeService nodeService;

    @PostMapping("/scan")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> scan() {
        int total = 0;
        for (int i = 0; i < 10; i++) {
            int c = scanner.scanOnePage();
            total += c;
            if (c == 0) break;
        }
        return ApiResponse.success();
    }

    @PostMapping("/enqueue/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> enqueue(
            @PathVariable("id") @NotNull(message = "ID不能为空")
            @Positive(message = "ID必须大于0")
            long id,
            @RequestParam(value = "idType", defaultValue = "node") String idType,
            @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive,
            @RequestParam(value = "deleteExisting", defaultValue = "true") boolean deleteExisting) {
        if (!systemProperties.getRobot().isEnabled()) return ApiResponse.success();
        if ("course".equals(idType)) {
            robotQueueService.enqueueByCourseId(id, contentType, recursive, deleteExisting);
        } else {
            robotQueueService.enqueue(id, contentType, recursive, deleteExisting);
        }
        return ApiResponse.success();
    }

    @PostMapping("/session/reset")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> resetSession() {
        aiService.resetSession();
        return ApiResponse.success();
    }

    @PostMapping("/session/summarize")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> summarizeSession() {
        aiService.summarizeCurrentSession();
        return ApiResponse.success();
    }

    @DeleteMapping("/queue")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> clearQueue() {
        long count = robotQueueService.clear();
        String message = count > 0 ?
            String.format("已清空队列，共删除 %d 个待处理节点", count) :
            "队列已经是空的";
        return ApiResponse.success(message);
    }

    @GetMapping("/queue/stats")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RobotQueueStatsDTO> getQueueStats() {
        RobotQueueStatsDTO stats = RobotQueueStatsDTO.builder()
            .pendingCount(robotQueueService.getPendingCount())
            .todayCompletedCount(robotQueueService.getTodayCompletedCount())
            .lastExecuteTime(robotQueueService.getLastExecuteTime())
            .status(robotQueueService.getStatus())
            .build();
        return ApiResponse.success(stats);
    }

    @PostMapping("/queue/pause")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> pauseQueue() {
        robotQueueService.pause();
        return ApiResponse.success();
    }

    @PostMapping("/queue/resume")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> resumeQueue() {
        robotQueueService.resume();
        return ApiResponse.success();
    }

    @GetMapping("/config")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RobotConfigDTO> getConfig() {
        SystemProperties.Robot robot = systemProperties.getRobot();
        RobotConfigDTO config = RobotConfigDTO.builder()
            .aiService(robot.getAiService())
            .model(robot.getModel())
            .build();
        return ApiResponse.success(config);
    }

    @PostMapping("/config")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateConfig(@RequestBody RobotConfigDTO config) {
        SystemProperties.Robot robot = systemProperties.getRobot();
        if (config.getAiService() != null) {
            robot.setAiService(config.getAiService());
        }
        if (config.getModel() != null) {
            robot.setModel(config.getModel());
        }
        return ApiResponse.success();
    }

    // DTO 类
    @lombok.Data
    @lombok.Builder
    public static class RobotConfigDTO {
        private String aiService;
        private String model;
    }

    // ========== 路径生成 ==========

    @PostMapping("/roadmap/generate/{roleId}")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RobotRoadmapTaskDTO> generateRoadmap(
            @PathVariable @NotNull @Positive Long roleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        String taskId = roadmapGenerationService.submitGenerateTask(roleId, userId);
        return ApiResponse.success(RobotRoadmapTaskDTO.builder()
            .taskId(taskId)
            .status("PENDING")
            .build());
    }

    @GetMapping("/roadmap/task/{taskId}")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<RobotRoadmapTaskDTO> getRoadmapTask(@PathVariable String taskId) {
        return ApiResponse.success(roadmapGenerationService.getTaskStatus(taskId));
    }

    @GetMapping("/roadmap/history")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<RobotRoadmapTaskDTO>> getRoadmapHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(roadmapGenerationService.getHistory(userId));
    }

    @PostMapping("/roadmap/draft")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> saveRoadmapDraft(
            @RequestParam @NotNull @Positive Long roleId,
            @RequestBody @NotBlank String draftContent) {
        Long userId = StpUtil.getLoginIdAsLong();
        String draftId = roadmapGenerationService.saveDraft(roleId, userId, draftContent);
        return ApiResponse.success(draftId);
    }

    @GetMapping("/roadmap/draft/{draftId}")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> getRoadmapDraft(@PathVariable @NotBlank String draftId) {
        String draft = roadmapGenerationService.getDraft(draftId);
        return ApiResponse.success(draft);
    }

    @DeleteMapping("/roadmap/draft/{draftId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> deleteRoadmapDraft(@PathVariable @NotBlank String draftId) {
        Long userId = StpUtil.getLoginIdAsLong();
        roadmapGenerationService.deleteDraft(userId, draftId);
        return ApiResponse.success();
    }

    @GetMapping("/roadmap/drafts")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<RobotRoadmapDraftDTO>> getRoadmapDrafts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ApiResponse.success(roadmapGenerationService.getDraftList(userId));
    }
}
