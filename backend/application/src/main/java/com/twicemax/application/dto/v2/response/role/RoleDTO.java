package com.twicemax.application.dto.v2.response.role;

import lombok.Data;

import java.time.Instant;

/**
 * v2 专业（Role）响应 DTO。
 *
 * <p>与 v1 {@code com.twicemax.application.dto.response.role.RoleDTO} 的区别：
 * 时间字段用 {@link Instant}，序列化为 ISO 8601 UTC 字符串（如 {@code 2026-04-25T10:30:00Z}）。
 */
@Data
public class RoleDTO {

    private Long id;

    private String name;

    private String description;

    private String price;

    private String skills;

    private Integer mainCategory;

    private Integer subCategory;

    /** 图标 */
    private String icon;

    /** 学习人数 */
    private Integer learnerCount;

    /** 是否已收藏（公开接口可能为 null） */
    private Boolean bookmarked;

    private Instant createdAt;
}
