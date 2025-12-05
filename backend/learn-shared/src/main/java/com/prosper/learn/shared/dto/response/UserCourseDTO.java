package com.prosper.learn.shared.dto.response;

import com.prosper.learn.shared.dto.response.course.CourseSummaryDTO;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserCourseDTO {

    private Long id;

    private Long userId;

    private Long courseId;

    /**
     * 课程摘要信息
     * 说明：包含课程的基本信息（id, name, description, 分类）
     */
    private CourseSummaryDTO course;

    private Integer progressPercent;

    private Byte state; // NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
