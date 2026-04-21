package com.twicemax.application.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 课程完成响应DTO
 */
@Data
@Builder
public class CourseCompletionResponseDTO {
    
    /**
     * 课程ID
     */
    private Long courseId;
    
    /**
     * 是否已完成
     */
    private Boolean completed;
    
    /**
     * 提示消息
     */
    private String message;
}