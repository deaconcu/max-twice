package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新课程复习策略请求DTO
 */
@Data
public class UpdateCourseSettingRequest {

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    private Integer frequencySetting;

    private Integer status;

    /**
     * 卡片顺序：0=先复习后新卡，1=先新卡后复习
     */
    private Integer cardOrder;

}