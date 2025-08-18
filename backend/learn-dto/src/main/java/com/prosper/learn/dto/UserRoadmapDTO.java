package com.prosper.learn.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRoadmapDTO {

    private Long id;

    private Long userId;

    private RoadmapDTOV2 roadmap;

    private Integer progressPercent;

    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
