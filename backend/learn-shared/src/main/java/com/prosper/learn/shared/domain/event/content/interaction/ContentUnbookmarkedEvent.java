package com.prosper.learn.shared.domain.event.content.interaction;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 取消内容收藏事件
 * 当用户取消收藏内容时触发
 */
@Data
@AllArgsConstructor
public class ContentUnbookmarkedEvent {

    /** 取消收藏者ID */
    private Long userId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;
}