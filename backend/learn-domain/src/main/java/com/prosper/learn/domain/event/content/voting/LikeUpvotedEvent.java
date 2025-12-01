package com.prosper.learn.domain.event.content.voting;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 点赞事件（原helpful）
 * 当用户给内容点赞时触发
 */
@Data
@AllArgsConstructor
public class LikeUpvotedEvent {

    /** 点赞者ID */
    private Long voterId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}