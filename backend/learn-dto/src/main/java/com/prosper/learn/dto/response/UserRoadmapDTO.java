package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.old.RoadmapDTOV1;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRoadmapDTO {

    private Long id;

    private Long userId;

    private RoadmapDTOV1 roadmap;

    private Integer progressPercent;

    private Byte state; // NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
