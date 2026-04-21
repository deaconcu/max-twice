package com.twicemax.application.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRoadmapDTO {

    private Long id;

    private Long userId;

    private Long roadmapId;

    private RoadmapDTO roadmap;

    private Integer progressPercent;

    private Byte state; // NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
