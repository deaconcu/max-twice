package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.old.CourseDTOV2;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserCourseDTO {

    private Long id;

    private Long userId;

    private CourseDTOV2 course;

    private Integer progressPercent;

    private Byte state; // NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
