package com.prosper.learn.business.event.content.lifecycle;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容删除事件
 * 当删除任何类型的内容时触发
 */
@Data
@AllArgsConstructor
public class ContentDeletedEvent {

    /** 操作者ID */
    private Long operatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}