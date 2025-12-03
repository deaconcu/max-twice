package com.prosper.learn.business.event.content.lifecycle;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 评论创建事件
 * 当用户创建评论时触发
 */
@Data
@AllArgsConstructor
public class CommentCreatedEvent {

    /** 评论者ID */
    private Long commenterId;

    /** 评论ID */
    private Long commentId;

    /** 被评论的内容ID */
    private Long contentId;

    /** 被评论的内容类型 */
    private ContentType contentType;

    /** 被评论内容的创建者ID */
    private Long contentCreatorId;
}