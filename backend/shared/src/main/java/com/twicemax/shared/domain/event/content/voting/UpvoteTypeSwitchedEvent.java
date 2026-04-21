package com.twicemax.shared.domain.event.content.voting;

import static com.twicemax.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 点赞类型切换事件
 * 当用户从一种点赞类型切换到另一种时触发
 * 例如：从 like 切换到 twice，或从 twice 切换到 like
 *
 * @param <T> 内容对象类型（PostDO, CommentDO, RoadmapDO等）
 */
@Data
@AllArgsConstructor
public class UpvoteTypeSwitchedEvent<T> {

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

    /** 原来的点赞类型（1-twice, 2-like） */
    private Integer fromType;

    /** 新的点赞类型（1-twice, 2-like） */
    private Integer toType;
}