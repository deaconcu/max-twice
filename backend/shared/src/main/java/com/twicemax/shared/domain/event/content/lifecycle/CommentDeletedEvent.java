package com.twicemax.shared.domain.event.content.lifecycle;

import static com.twicemax.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 评论删除事件
 * 当评论被删除时触发
 */
@Data
@AllArgsConstructor
public class CommentDeletedEvent {

    /** 操作者ID */
    private Long operatorId;

    /** 评论ID */
    private Long commentId;

    /** 被评论的内容ID */
    private Long contentId;

    /** 被评论的内容类型 */
    private ContentType contentType;

    /** 被评论内容的创建者ID */
    private Long contentCreatorId;
}