package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
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
     * 主分类ID。创建主课程时必填；创建子课程时（parentCourseId>0）可省略，
     * 由服务端从父课程继承。
     */
    @Positive
    private Integer mainCategory;

    /**
     * 子分类ID。创建主课程时必填；创建子课程时（parentCourseId>0）可省略，
     * 由服务端从父课程继承。
     */
    @Positive
    private Integer subCategory;

    /**
     * 父课程ID。null 或 0 表示创建主课程；> 0 表示创建子课程，
     * 此时 mainCategory / subCategory 会从父课程继承，传入值被忽略。
     */
    private Long parentCourseId;
}