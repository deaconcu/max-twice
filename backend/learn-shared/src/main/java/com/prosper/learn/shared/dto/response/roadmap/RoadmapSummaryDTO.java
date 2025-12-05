package com.prosper.learn.shared.dto.response.roadmap;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路线图摘要 DTO
 *
 * 用途：基础路线图信息
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class RoadmapSummaryDTO {

    private Long id;

    private String content;

    private Long professionId;

    private String description;

    private Byte state;

    private Integer vote;

    private Integer comment;

    private Long creatorId;

    /**
     * 内容是否可用
     * true: 内容可用
     * false: 内容不可用（已删除、已屏蔽、已拒绝等）
     * null: 默认可用
     */
    private Boolean available;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
