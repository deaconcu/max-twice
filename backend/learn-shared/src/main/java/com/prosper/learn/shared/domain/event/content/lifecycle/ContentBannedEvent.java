package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentState;
import static com.prosper.learn.shared.domain.Enums.ContentType;
import static com.prosper.learn.shared.domain.Enums.PostType;
import lombok.Data;

import java.util.List;

/**
 * 内容封禁事件
 * 当内容被永久封禁时触发（SUBMITTED/PUBLISHED → BANNED）
 * 用于发送封禁通知、减少统计数据
 *
 * 注意：BAN 操作需要减少对象和用户维度的统计（如果之前是 PUBLISHED 状态）
 * 适用于：所有内容类型
 */
@Data
public class ContentBannedEvent {

    /** 创建者ID */
    private Long creatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 之前的状态（用于判断是否需要减少统计） */
    private ContentState previousState;

    /** 节点ID（post 类型使用）*/
    private Long nodeId;

    /** 帖子类型（post 类型使用）*/
    private PostType postType;

    /** 目录型帖子引用的节点ID列表（post 类型且 postType=CONTENTS 时使用）*/
    private List<Long> referencedNodeIds;

    /** 职业ID（roadmap 类型使用）*/
    private Long professionId;

    /** 帖子ID（memory_card_deck 类型使用，可为null）*/
    private Long postId;

    /** 被评论对象的类型（comment 类型使用）*/
    private ContentType commentTargetType;

    /** 被评论对象的ID（comment 类型使用）*/
    private Long commentTargetId;

    /** 封禁原因 */
    private String reason;

    // 用于通知的字段
    private String postContentPreview;
    private String nodeName;
    private String courseName;
    private String professionName;

    // ========== 各类型专用构造函数 ==========

    /** Post 类型构造函数 */
    public static ContentBannedEvent forPost(Long creatorId, Long postId, ContentState previousState, Long nodeId, PostType postType,
                                             String postPreview, String nodeName, String courseName, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.previousState = previousState;
        event.nodeId = nodeId;
        event.postType = postType;
        event.postContentPreview = postPreview;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Index 类型 Post 构造函数（目录型帖子，只包含必要字段）*/
    public static ContentBannedEvent forIndexPost(Long creatorId, Long postId, ContentState previousState, Long nodeId, String reason, List<Long> referencedNodeIds) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.previousState = previousState;
        event.nodeId = nodeId;
        event.postType = PostType.index;
        event.reason = reason;
        event.referencedNodeIds = referencedNodeIds;
        return event;
    }

    /** Roadmap 类型构造函数 */
    public static ContentBannedEvent forRoadmap(Long creatorId, Long roadmapId, ContentState previousState, Long professionId,
                                                String professionName, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = roadmapId;
        event.contentType = ContentType.roadmap;
        event.previousState = previousState;
        event.professionId = professionId;
        event.professionName = professionName;
        event.reason = reason;
        return event;
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentBannedEvent forMemoryCardDeck(Long creatorId, Long deckId, ContentState previousState, Long postId, Long nodeId,
                                                       String postPreview, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = deckId;
        event.contentType = ContentType.memory_card_deck;
        event.previousState = previousState;
        event.postId = postId;
        event.nodeId = nodeId;
        event.postContentPreview = postPreview;
        event.reason = reason;
        return event;
    }

    /** Comment 类型构造函数 */
    public static ContentBannedEvent forComment(Long creatorId, Long commentId, ContentState previousState,
                                                ContentType commentTargetType, Long commentTargetId, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = commentId;
        event.contentType = ContentType.comment;
        event.previousState = previousState;
        event.commentTargetType = commentTargetType;
        event.commentTargetId = commentTargetId;
        event.reason = reason;
        return event;
    }

    /** Course 类型构造函数 */
    public static ContentBannedEvent forCourse(Long creatorId, Long courseId, String courseName, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = courseId;
        event.contentType = ContentType.course;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Profession 类型构造函数 */
    public static ContentBannedEvent forProfession(Long creatorId, Long professionId, String professionName, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = professionId;
        event.contentType = ContentType.profession;
        event.professionName = professionName;
        event.reason = reason;
        return event;
    }

    /** Node 类型构造函数 */
    public static ContentBannedEvent forNode(Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName, String reason) {
        ContentBannedEvent event = new ContentBannedEvent();
        event.creatorId = creatorId;
        event.contentId = nodeId;
        event.contentType = ContentType.node;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }
}
