package com.prosper.learn.application.dto.response.comment;

import lombok.Data;

/**
 * 评论摘要 DTO
 *
 * 用途：管理端评论列表、基础评论信息展示
 *
 * 使用场景：
 * - 管理员查看待审核评论列表
 * - 管理员查看已拒绝/已封禁评论列表
 * - 不需要用户详细信息和点赞状态的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CommentSummaryDTO {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 评论内容（原始文本或Markdown）
     */
    private String content;

    /**
     * 评论对象类型
     * - 0: 帖子（Post）
     * - 1: 节点（Node）
     */
    private Integer objectType;

    /**
     * 评论对象ID（帖子ID或节点ID）
     */
    private Long objectId;

    /**
     * 回复数量
     * 该评论下的直接回复总数
     */
    private Integer replyCount;

    /**
     * 回复的评论ID
     * - 如果是顶级评论，则为 null
     * - 如果是回复某条评论，则为被回复评论的ID
     */
    private Long replyToCommentId;

    /**
     * 创建者用户ID
     * 发表该评论的用户ID
     */
    private Long creatorId;

    /**
     * 被回复的用户ID
     * - 如果是顶级评论，则为 null
     * - 如果是回复某条评论，则为被回复评论的作者ID
     */
    private Long toUserId;

    /**
     * 点赞数
     * 该评论获得的点赞总数
     */
    private Integer upvoteCount;

    /**
     * 评论状态
     * - 0: 待审核（PENDING）
     * - 1: 已通过（APPROVED）
     * - 2: 已拒绝（REJECTED）
     * - 3: 已封禁（BANNED）
     */
    private Integer state;

    /**
     * 评论分数
     * 用于排序的分数，通常基于点赞数、时间等因素计算
     */
    private Double score;

    /**
     * 创建时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    private String createdAt;
}
