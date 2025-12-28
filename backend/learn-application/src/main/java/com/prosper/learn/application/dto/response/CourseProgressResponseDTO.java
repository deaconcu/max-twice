package com.prosper.learn.application.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 课程进度响应DTO
 * 用于开始/取消学习课程的响应
 */
@Data
@Builder
public class CourseProgressResponseDTO {

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 是否正在学习
     */
    private Boolean learning;
}
