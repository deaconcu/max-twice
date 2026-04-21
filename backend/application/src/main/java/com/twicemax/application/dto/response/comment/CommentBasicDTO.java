package com.twicemax.application.dto.response.comment;

import lombok.Data;

/**
 * 评论基本信息 DTO
 * 用于获取评论的基本信息，如所属对象类型和ID
 */
@Data
public class CommentBasicDTO {
    /**
     * 评论ID
     */
    private Long id;

    /**
     * 评论所属对象类型 (1=post, 2=node, 3=roadmap)
     */
    private Integer objectType;

    /**
     * 评论所属对象ID
     */
    private Long objectId;

    /**
     * 如果是子评论，父评论ID
     */
    private Long replyToCommentId;
}
