package com.prosper.learn.domain.event.content.lifecycle;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容创建事件
 * 当创建任何类型的内容时触发（文章、帖子、评论、路线图、课程等）
 */
@Data
@AllArgsConstructor
public class ContentCreatedEvent {

    /** 创建者ID */
    private Long creatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;
}