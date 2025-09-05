package com.prosper.learn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新课程请求DTO
 * 
 * @author Claude Code
 */
@Data
public class UpdateCourseRequest {

    /**
     * 课程名称
     */
    @NotBlank(message = "课程名称不能为空")
    @Size(max = 100, message = "课程名称长度不能超过100字符")
    private String name;

    /**
     * 课程描述
     */
    @NotBlank(message = "课程描述不能为空")
    @Size(max = 500, message = "课程描述长度不能超过500字符")
    private String description;

    /**
     * 主分类ID
     */
    @NotNull(message = "主分类不能为空")
    private Integer mainCategory;

    /**
     * 子分类ID
     */
    @NotNull(message = "子分类不能为空")
    private Integer subCategory;
}