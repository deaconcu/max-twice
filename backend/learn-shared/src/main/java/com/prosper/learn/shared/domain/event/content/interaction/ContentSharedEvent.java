package com.prosper.learn.shared.domain.event.content.interaction;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容分享事件
 * 当用户分享内容时触发
 */
@Data
@AllArgsConstructor
public class ContentSharedEvent {

    /** 分享者ID */
    private Long sharerId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}