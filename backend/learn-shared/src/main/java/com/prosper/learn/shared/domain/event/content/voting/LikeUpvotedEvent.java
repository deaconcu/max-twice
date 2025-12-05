package com.prosper.learn.shared.domain.event.content.voting;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 点赞事件（原helpful）
 * 当用户给内容点赞时触发
 *
 * @param <T> 内容对象类型（PostDO, CommentDO, RoadmapDO等）
 */
@Data
@AllArgsConstructor
public class LikeUpvotedEvent<T> {

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

    /** 上下文ID - 对于Post/Comment是nodeId，对于Roadmap是professionId */
    private Long contextId;
}