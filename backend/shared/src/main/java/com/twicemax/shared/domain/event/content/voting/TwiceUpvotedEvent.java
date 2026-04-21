package com.twicemax.shared.domain.event.content.voting;

import static com.twicemax.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 两次能懂点赞事件
 * 当用户给内容点"两次能懂"时触发
 *
 * @param <T> 内容对象类型（PostDO, CommentDO, RoadmapDO等）
 */
@Data
@AllArgsConstructor
public class TwiceUpvotedEvent<T> {

    /** 点赞者ID */
    private Long voterId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 内容创建者ID */
    private Long creatorId;

    /** 内容对象 */
    private T contentObject;
}