package com.prosper.learn.shared.dto.response.userroadmap;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户路线图摘要 DTO
 *
 * 用途：基础学习记录（不含路线图详细信息）
 * 使用场景：统计、列表展示等不需要路线图详细信息的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class UserRoadmapSummaryDTO {

    /**
     * 学习记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 路线图ID
     */
    private Long roadmapId;

    /**
     * 学习进度百分比 (0-100)
     */
    private Integer progressPercent;

    /**
     * 学习状态
     * 0 = NOT_STARTED（未开始）
     * 1 = IN_PROGRESS（进行中）
     * 2 = COMPLETED（已完成）
     */
    private Byte state;

    /**
     * 开始学习时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
