package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新角色请求DTO
 *
 * @author Claude Code
 */
@Data
public class UpdateRoleRequest {

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100字符")
    private String name;

    /**
     * 角色描述
     */
    @Size(max = 500, message = "角色描述长度不能超过500字符")
    private String description;

    /**
     * 价格
     */
    @Size(max = 50, message = "价格长度不能超过50字符")
    private String price;

    /**
     * 技能要求
     */
    @Size(max = 1000, message = "技能要求长度不能超过1000字符")
    private String skills;

    /**
     * 主分类ID
     */
    @Positive(message = "主分类ID必须大于0")
    private Integer mainCategory;

    /**
     * 子分类ID
     */
    @Positive(message = "子分类ID必须大于0")
    private Integer subCategory;

    /**
     * 图标
     */
    @Size(max = 100, message = "图标长度不能超过100字符")
    private String icon;

    /**
     * 拒绝原因
     */
    @Size(max = 500, message = "操作原因长度不能超过500字符")
    private String reason;
}