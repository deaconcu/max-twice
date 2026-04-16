package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import static com.prosper.learn.shared.domain.Enums.PostType;
import lombok.Data;

import java.util.List;

/**
 * 内容下架事件
 * 当已发布内容被下架时触发（PUBLISHED → REJECTED）
 * 用于发送下架通知、减少统计数据
 *
 * 注意：REMOVE 操作会减少对象和用户维度的统计
 * 仅适用于：Post、Roadmap、MemoryCardDeck
 */
@Data
public class ContentRemovedEvent {

    /** 创建者ID */
    private Long creatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 节点ID（post 类型使用）*/
    private Long nodeId;

    /** 帖子类型（post 类型使用）*/
    private PostType postType;

    /** 目录型帖子引用的节点ID列表（post 类型且 postType=CONTENTS 时使用）*/
    private List<Long> referencedNodeIds;

    /** 角色ID（roadmap 类型使用）*/
    private Long roleId;

    /** 帖子ID（memory_card_deck 类型使用）*/
    private Long postId;

    /** 下架原因 */
    private String reason;

    /** 帖子预览（用于通知）*/
    private String postContentPreview;

    /** 节点名称（用于通知）*/
    private String nodeName;

    /** 课程名称（用于通知）*/
    private String courseName;

    /** 角色名称（用于通知）*/
    private String roleName;

    // ========== 各类型专用构造函数 ==========

    /** Post 类型构造函数 */
    public static ContentRemovedEvent forPost(Long creatorId, Long postId, Long nodeId, PostType postType,
                                              String postPreview, String nodeName, String courseName, String reason) {
        ContentRemovedEvent event = new ContentRemovedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.nodeId = nodeId;
        event.postType = postType;
        event.postContentPreview = postPreview;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Index 类型 Post 构造函数（目录型帖子，只包含必要字段）*/
    public static ContentRemovedEvent forIndexPost(Long creatorId, Long postId, Long nodeId, String reason, List<Long> referencedNodeIds) {
        ContentRemovedEvent event = new ContentRemovedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.nodeId = nodeId;
        event.postType = PostType.index;
        event.reason = reason;
        event.referencedNodeIds = referencedNodeIds;
        return event;
    }

    /** Roadmap 类型构造函数 */
    public static ContentRemovedEvent forRoadmap(Long creatorId, Long roadmapId, Long roleId,
                                                 String roleName, String reason) {
        ContentRemovedEvent event = new ContentRemovedEvent();
        event.creatorId = creatorId;
        event.contentId = roadmapId;
        event.contentType = ContentType.roadmap;
        event.roleId = roleId;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentRemovedEvent forMemoryCardDeck(Long creatorId, Long deckId, Long postId, Long nodeId,
                                                        String postPreview, String reason) {
        ContentRemovedEvent event = new ContentRemovedEvent();
        event.creatorId = creatorId;
        event.contentId = deckId;
        event.contentType = ContentType.memory_card_deck;
        event.postId = postId;
        event.nodeId = nodeId;
        event.postContentPreview = postPreview;
        event.reason = reason;
        return event;
    }
}
