package com.prosper.learn.domain.event.content.voting;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 取消点赞事件
 * 当用户取消点赞时触发
 */
@Data
@AllArgsConstructor
public class LikeUpvoteCancelledEvent {

    /** 取消点赞者ID */
    private Long voterId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}