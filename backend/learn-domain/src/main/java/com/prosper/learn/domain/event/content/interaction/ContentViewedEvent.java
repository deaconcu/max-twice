package com.prosper.learn.domain.event.content.interaction;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容浏览事件
 * 当用户浏览任何类型的内容时触发
 */
@Data
@AllArgsConstructor
public class ContentViewedEvent {

    /** 浏览者ID */
    private Long viewerId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}