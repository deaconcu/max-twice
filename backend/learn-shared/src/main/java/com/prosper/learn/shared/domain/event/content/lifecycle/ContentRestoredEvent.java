package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;

/**
 * 内容恢复事件
 * 当内容被恢复发布时触发（REJECTED/BANNED → PUBLISHED）
 * 用于发送恢复通知、恢复统计数据
 *
 * 注意：RESTORE 操作需要根据之前的状态决定是否恢复统计
 * - 从 REJECTED 恢复：如果之前是 REMOVE 下架的，需要恢复统计
 * - 从 BANNED 恢复：需要恢复统计
 */
@Data
public class ContentRestoredEvent {

    /** 操作者ID */
    private Long operatorId;

    /** 创建者ID */
    private Long creatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 之前的状态（REJECTED=3 或 BANNED=4）*/
    private Byte previousState;

    /** 节点ID（post 类型使用）*/
    private Long nodeId;

    /** 帖子类型（post 类型使用，1=ARTICLE, 2=INDEX）*/
    private Integer postType;

    /** 职业ID（roadmap 类型使用）*/
    private Long professionId;

    /** 帖子ID（memory_card_deck 类型使用，可为null）*/
    private Long postId;

    /** 被评论对象的类型（comment 类型使用）*/
    private ContentType commentTargetType;

    /** 被评论对象的ID（comment 类型使用）*/
    private Long commentTargetId;

    /** 恢复原因 */
    private String reason;

    // 用于通知的字段
    private String postContentPreview;
    private String nodeName;
    private String courseName;
    private String professionName;

    // ========== 各类型专用构造函数 ==========

    /** Post 类型构造函数 */
    public static ContentRestoredEvent forPost(Long operatorId, Long creatorId, Long postId, Byte previousState,
                                               Long nodeId, Integer postType, String postPreview, String nodeName,
                                               String courseName, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
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

    /** Roadmap 类型构造函数 */
    public static ContentRestoredEvent forRoadmap(Long operatorId, Long creatorId, Long roadmapId, Byte previousState,
                                                  Long professionId, String professionName, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
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
    public static ContentRestoredEvent forMemoryCardDeck(Long operatorId, Long creatorId, Long deckId, Byte previousState,
                                                         Long postId, Long nodeId, String postPreview, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
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
    public static ContentRestoredEvent forComment(Long operatorId, Long creatorId, Long commentId, Byte previousState,
                                                  ContentType commentTargetType, Long commentTargetId, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
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
    public static ContentRestoredEvent forCourse(Long operatorId, Long creatorId, Long courseId, String courseName, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
        event.creatorId = creatorId;
        event.contentId = courseId;
        event.contentType = ContentType.course;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Profession 类型构造函数 */
    public static ContentRestoredEvent forProfession(Long operatorId, Long creatorId, Long professionId, String professionName, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
        event.creatorId = creatorId;
        event.contentId = professionId;
        event.contentType = ContentType.profession;
        event.professionName = professionName;
        event.reason = reason;
        return event;
    }

    /** Node 类型构造函数 */
    public static ContentRestoredEvent forNode(Long operatorId, Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName, String reason) {
        ContentRestoredEvent event = new ContentRestoredEvent();
        event.operatorId = operatorId;
        event.creatorId = creatorId;
        event.contentId = nodeId;
        event.contentType = ContentType.node;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }
}
