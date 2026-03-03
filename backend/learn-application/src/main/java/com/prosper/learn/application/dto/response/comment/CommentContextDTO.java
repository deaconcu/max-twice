package com.prosper.learn.application.dto.response.comment;

import lombok.Data;

import java.util.List;

/**
 * 评论上下文 DTO
 *
 * 用途：根据评论ID获取该评论及其上下文（前后评论）
 *
 * 使用场景：
 * - 从外部链接跳转到特定评论（如 /read?commentId=123）
 * - 需要高亮显示特定评论并展示其前后评论
 */
@Data
public class CommentContextDTO {

    /**
     * 评论列表（包含目标评论及其前后评论）
     * - 主评论上下文：List<CommentWithRepliesDTO>
     * - 子评论上下文：List<CommentDetailDTO>（通过 subItems 返回）
     * 按 score DESC, id DESC 排序（热度高的在前）
     */
    private List<CommentWithRepliesDTO> items;

    /**
     * 子评论列表（仅当查询子评论上下文时返回）
     */
    private List<CommentDetailDTO> subItems;

    /**
     * 目标评论ID（用于前端定位高亮）
     */
    private Long targetCommentId;

    /**
     * 父评论ID（仅当查询子评论上下文时返回，用于前端定位）
     */
    private Long parentCommentId;

    /**
     * 前面是否还有更多评论
     */
    private Boolean hasMoreBefore;

    /**
     * 后面是否还有更多评论
     */
    private Boolean hasMoreAfter;

    /**
     * 向前加载的游标（第一条评论的 score 和 id）
     */
    private Double firstScore;
    private Long firstId;

    /**
     * 向后加载的游标（最后一条评论的 score 和 id）
     */
    private Double lastScore;
    private Long lastId;
}
