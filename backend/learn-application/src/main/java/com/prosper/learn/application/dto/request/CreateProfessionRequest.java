package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建职业请求DTO
 *
 * @author Claude Code
 */
@Data
public class CreateProfessionRequest {

    /**
     * 职业名称
     */
    @NotBlank(message = "职业名称不能为空")
    @Size(max = 100, message = "职业名称长度不能超过100字符")
    private String name;

    /**
     * 职业描述
     */
    @NotBlank(message = "职业描述不能为空")
    @Size(max = 500, message = "职业描述长度不能超过500字符")
    private String description;

    /**
     * 主分类ID
     */
    @NotNull(message = "主分类不能为空")
    @Positive(message = "主分类ID必须大于0")
    private Integer mainCategory;

    /**
     * 子分类ID
     */
    @NotNull(message = "子分类不能为空")
    @Positive(message = "子分类ID必须大于0")
    private Integer subCategory;

    /**
     * 技能要求
     */
    @Size(max = 1000, message = "技能要求长度不能超过1000字符")
    private String skills;
}