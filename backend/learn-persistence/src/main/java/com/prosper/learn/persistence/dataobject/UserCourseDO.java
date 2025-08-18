package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseDO {

    private Long id;

    private Long userId;

    private Long courseId;

    private Integer progressPercent;

    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}