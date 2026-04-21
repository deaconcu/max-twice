package com.twicemax.application.dto.response.user;

import lombok.Data;

/**
 * 用户摘要 DTO
 *
 * 用途：公开的用户基本信息
 *
 * 使用场景：
 * - 用户列表展示
 * - 评论、帖子的作者信息
 * - 任何需要显示用户基本信息的场景
 *
 * 替代关系：
 * - 替代原 V1（id + name + state + biography）
 *
 * 安全性：
 * - ✅ 不包含敏感信息（password, phone, email）
 * - ✅ 适合公开展示
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class UserSummaryDTO {

    /**
     * 用户ID
     * 说明：用户的唯一标识
     */
    private Long id;

    /**
     * 用户名
     * 说明：用户的显示名称
     */
    private String name;

    /**
     * 用户状态
     * 说明：NORMAL(0-正常), BANNED(1-已封禁)
     * 何时填充：始终填充，用于判断用户是否可用
     */
    private Byte state;

    /**
     * 个人简介
     * 说明：用户的自我介绍
     * 何时填充：在需要显示个人信息时填充
     */
    private String biography;

    /**
     * 用户头像
     * 说明：用户的头像 URL
     * 何时填充：在需要显示用户头像时填充
     */
    private String avatar;
}
