package com.twicemax.application.dto.response.user;

import lombok.Data;

/**
 * 用户简要 DTO
 *
 * 用途：极简用户信息，包含 ID、名称和头像
 *
 * 使用场景：
 * - 评论作者信息（CommentDetailDTO.creatorName 可以改为嵌套此DTO）
 * - 帖子作者信息
 * - 任何需要显示用户名和头像的场景
 *
 * 替代关系：
 * - 替代原 V2（id + name + state + role）的部分场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class UserBriefDTO {

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
     * 用户头像
     * 说明：用户的头像 URL
     */
    private String avatar;
}
