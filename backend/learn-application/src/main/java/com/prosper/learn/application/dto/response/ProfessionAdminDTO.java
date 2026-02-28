package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 职业管理后台 DTO
 * 包含管理员需要的字段（如状态、拒绝原因等）
 */
@Data
public class ProfessionAdminDTO {

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
}
