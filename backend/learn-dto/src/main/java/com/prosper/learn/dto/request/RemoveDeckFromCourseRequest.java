package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 移除卡片组请求DTO
 */
@Data
public class RemoveDeckFromCourseRequest {

    @NotNull(message = "卡片组ID不能为空")
    private Long deckId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

}