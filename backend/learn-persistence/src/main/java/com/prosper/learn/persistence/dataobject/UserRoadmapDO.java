package com.prosper.learn.persistence.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserRoadmapDO {

    private Long id;

    private Long userId;

    private Long roadmapId;

    private Integer progressPercent;

    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
