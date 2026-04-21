package com.twicemax.application.dto.response.role;

import com.twicemax.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 角色管理后台 DTO
 * 包含管理员需要的字段（如状态、拒绝原因等）
 */
@Data
public class RoleAdminDTO {

    private Long id;

    private String name;

    private String description;

    private String price;

    private String skills;

    private Integer mainCategory;

    private Integer subCategory;

    private String icon;

    private Byte state;

    private String reason;

    private UserBriefDTO creator;

    private String createdAt;

    private String updatedAt;

    // 统计字段
    private Integer roadmapCount;

    private Integer bookmarkCount;
}
