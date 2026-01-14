package com.prosper.learn.application.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

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

    private Byte state; // 职业状态（管理员可见）

    private String reason; // 拒绝/封禁原因（管理员可见）

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
