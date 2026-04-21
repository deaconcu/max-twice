package com.twicemax.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * 每日新卡上限
     */
    @Min(value = 0, message = "每日新卡上限不能小于0")
    @Max(value = 999, message = "每日新卡上限不能超过999")
    private Integer dailyNewLimit;

    /**
     * 每日复习上限
     */
    @Min(value = 0, message = "每日复习上限不能小于0")
    @Max(value = 9999, message = "每日复习上限不能超过9999")
    private Integer dailyReviewLimit;

}