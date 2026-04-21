package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 创建课程请求DTO
 * 
 * @author Claude Code
 */
@Data
public class CreateCourseRequest {

    /**
     * 课程名称
     */
    @NotBlank(message = "课程名称不能为空")
    @ConfigurableSize(configKey = "course-name")
    private String name;

    /**
     * 课程描述
     */
    @NotBlank(message = "课程描述不能为空")
    @ConfigurableSize(configKey = "course-description")
    private String description;

    /**
     * 主分类ID
     */
    @NotNull(message = "主分类不能为空")
    @Positive
    private Integer mainCategory;

    /**
     * 子分类ID
     */
    @NotNull(message = "子分类不能为空")
    @Positive
    private Integer subCategory;
}