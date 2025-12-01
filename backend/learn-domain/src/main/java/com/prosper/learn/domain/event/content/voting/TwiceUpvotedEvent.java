package com.prosper.learn.domain.event.content.voting;

import com.prosper.learn.common.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 两次能懂点赞事件
 * 当用户给内容点"两次能懂"时触发
 */
@Data
@AllArgsConstructor
public class TwiceUpvotedEvent {

    /** 点赞者ID */
    private Long voterId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;
}