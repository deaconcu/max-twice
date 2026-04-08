package com.prosper.learn.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Robot 路径生成任务状态 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotRoadmapTaskDTO {
    private String taskId;
    private Long roleId;  // 职业ID
    private Long userId;        // 用户ID
    private String status;      // PENDING, COMPLETED, FAILED
    private String result;      // JSON字符串（前端解析）
    private String error;       // 错误信息
    private String createdAt;
    private String completedAt;
}
