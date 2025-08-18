package com.prosper.learn.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserCourseDTO {

    private Long id;

    private Long userId;

    private CourseDTOV2 course;

    private Integer progressPercent;

    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
