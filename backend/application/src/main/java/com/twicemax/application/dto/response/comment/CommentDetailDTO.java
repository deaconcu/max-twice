package com.twicemax.application.dto.response.comment;

import com.twicemax.application.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论详情 DTO
 *
 * 用途：用户端评论详情展示、评论回复列表
 *
 * 使用场景：
 * - 用户查看评论详情（需要显示用户名和头像）
 * - 获取某条评论的回复列表（需要显示点赞状态）
 * - 创建评论后返回完整信息
 *
 * 与 CommentSummaryDTO 的区别：
 * - 新增：creator（创建者信息，包含ID、用户名、头像）
 * - 新增：toUser（被回复用户信息，包含ID、用户名、头像）
 * - 新增：liked（当前用户是否已点赞）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentDetailDTO extends CommentSummaryDTO {

    /**
     * 创建者信息
     * 动态填充：通过 creatorId 查询用户信息获得
     * 包含：用户ID、用户名、头像URL
     */
    private UserBriefDTO creator;

    /**
     * 被回复用户信息
     * 动态填充：通过 toUserId 查询用户信息获得
     * 包含：用户ID、用户名、头像URL
     * - 如果是顶级评论，则为 null
     */
    private UserBriefDTO toUser;

    /**
     * 当前用户是否已点赞
     * 动态填充：根据当前登录用户ID和评论ID查询点赞关系
     * - true: 已点赞
     * - false: 未点赞
     * - null: 未登录用户
     */
    private Boolean liked;
}
