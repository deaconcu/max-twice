package com.twicemax.web.v2.controller.admin;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Robot 内容生成管理接口
 */
@RestController
@RequestMapping("/admin/robot")
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
    public ResponseEntity<Void> scan() {
        for (int i = 0; i < 10; i++) {
            int c = scanner.scanOnePage();
            if (c == 0) break;
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/enqueue/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> enqueue(
            @PathVariable("id") @NotNull(message = "ID不能为空")
            @Positive(message = "ID必须大于0")
            long id,
            @RequestParam(value = "idType", defaultValue = "node") String idType,
            @RequestParam(value = "contentType", defaultValue = "auto") String contentType,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive,
            @RequestParam(value = "deleteExisting", defaultValue = "true") boolean deleteExisting) {
        if (systemProperties.getRobot().isEnabled()) {
            if ("course".equals(idType)) {
                robotQueueService.enqueueByCourseId(id, contentType, recursive, deleteExisting);
            } else {
                robotQueueService.enqueue(id, contentType, recursive, deleteExisting);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/session/reset")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> resetSession() {
        aiService.resetSession();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/session/summarize")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> summarizeSession() {
        aiService.summarizeCurrentSession();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/queue")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String clearQueue() {
        long count = robotQueueService.clear();
        return count > 0 ?
            String.format("已清空队列，共删除 %d 个待处理节点", count) :
            "队列已经是空的";
    }

    @GetMapping("/queue/stats")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RobotQueueStatsDTO getQueueStats() {
        return RobotQueueStatsDTO.builder()
            .pendingCount(robotQueueService.getPendingCount())
            .todayCompletedCount(robotQueueService.getTodayCompletedCount())
            .lastExecuteTime(robotQueueService.getLastExecuteTime())
            .status(robotQueueService.getStatus())
            .build();
    }

    @PostMapping("/queue/pause")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> pauseQueue() {
        robotQueueService.pause();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/queue/resume")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> resumeQueue() {
        robotQueueService.resume();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/config")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RobotConfigDTO getConfig() {
        SystemProperties.Robot robot = systemProperties.getRobot();
        return RobotConfigDTO.builder()
            .aiService(robot.getAiService())
            .model(robot.getModel())
            .build();
    }

    @PostMapping("/config")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateConfig(@RequestBody RobotConfigDTO config) {
        SystemProperties.Robot robot = systemProperties.getRobot();
        if (config.getAiService() != null) {
            robot.setAiService(config.getAiService());
        }
        if (config.getModel() != null) {
            robot.setModel(config.getModel());
        }
        return ResponseEntity.noContent().build();
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
    public RobotRoadmapTaskDTO generateRoadmap(
            @PathVariable @NotNull @Positive Long roleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        String taskId = roadmapGenerationService.submitGenerateTask(roleId, userId);
        return RobotRoadmapTaskDTO.builder()
            .taskId(taskId)
            .status("PENDING")
            .build();
    }

    @GetMapping("/roadmap/task/{taskId}")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RobotRoadmapTaskDTO getRoadmapTask(@PathVariable String taskId) {
        return roadmapGenerationService.getTaskStatus(taskId);
    }

    @GetMapping("/roadmap/history")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RobotRoadmapTaskDTO> getRoadmapHistory() {
        Long userId = StpUtil.getLoginIdAsLong();
        return roadmapGenerationService.getHistory(userId);
    }

    @PostMapping("/roadmap/draft")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String saveRoadmapDraft(
            @RequestParam @NotNull @Positive Long roleId,
            @RequestBody @NotBlank String draftContent) {
        Long userId = StpUtil.getLoginIdAsLong();
        return roadmapGenerationService.saveDraft(roleId, userId, draftContent);
    }

    @GetMapping("/roadmap/draft/{draftId}")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String getRoadmapDraft(@PathVariable @NotBlank String draftId) {
        return roadmapGenerationService.getDraft(draftId);
    }

    @DeleteMapping("/roadmap/draft/{draftId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> deleteRoadmapDraft(@PathVariable @NotBlank String draftId) {
        Long userId = StpUtil.getLoginIdAsLong();
        roadmapGenerationService.deleteDraft(userId, draftId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roadmap/drafts")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RobotRoadmapDraftDTO> getRoadmapDrafts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return roadmapGenerationService.getDraftList(userId);
    }
}
