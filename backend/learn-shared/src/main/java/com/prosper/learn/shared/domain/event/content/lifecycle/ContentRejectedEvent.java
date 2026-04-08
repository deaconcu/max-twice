package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;

/**
 * 内容审核拒绝事件
 * 当内容审核不通过时触发（SUBMITTED → REJECTED）
 * 用于发送拒绝通知，告知用户拒绝原因
 * 注意：REJECT 操作不需要减少统计，因为内容从未发布过
 */
@Data
public class ContentRejectedEvent {

    /** 创建者ID（消息接收者）*/
    private Long creatorId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private ContentType contentType;

    /** 课程名称（course 类型使用）*/
    private String courseName;

    /** 节点名称（node 类型使用）*/
    private String nodeName;

    /** 课程ID（node 类型使用）*/
    private Long courseId;

    /** 帖子预览（post 类型使用）*/
    private String postContentPreview;

    /** 节点ID（post 类型使用）*/
    private Long nodeId;

    /** 职业名称（role/roadmap 类型使用）*/
    private String roleName;

    /** 职业ID（roadmap 类型使用）*/
    private Long roleId;

    /** 帖子ID（memory_card_deck 类型使用）*/
    private Long postId;

    /** 拒绝原因 */
    private String reason;

    // ========== 各类型专用构造函数 ==========

    /** Role 类型构造函数 */
    public static ContentRejectedEvent forRole(Long creatorId, Long roleId, String roleName, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = roleId;
        event.contentType = ContentType.role;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }

    /** Course 类型构造函数 */
    public static ContentRejectedEvent forCourse(Long creatorId, Long courseId, String courseName, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = courseId;
        event.contentType = ContentType.course;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Node 类型构造函数 */
    public static ContentRejectedEvent forNode(Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = nodeId;
        event.contentType = ContentType.node;
        event.nodeName = nodeName;
        event.courseId = courseId;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Post 类型构造函数 */
    public static ContentRejectedEvent forPost(Long creatorId, Long postId, String postPreview, Long nodeId, String nodeName, String courseName, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.postContentPreview = postPreview;
        event.nodeId = nodeId;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.reason = reason;
        return event;
    }

    /** Roadmap 类型构造函数 */
    public static ContentRejectedEvent forRoadmap(Long creatorId, Long roadmapId, Long roleId, String roleName, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = roadmapId;
        event.contentType = ContentType.roadmap;
        event.roleId = roleId;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentRejectedEvent forMemoryCardDeck(Long creatorId, Long deckId, Long postId, String postContentPreview, String reason) {
        ContentRejectedEvent event = new ContentRejectedEvent();
        event.creatorId = creatorId;
        event.contentId = deckId;
        event.contentType = ContentType.memory_card_deck;
        event.postId = postId;
        event.postContentPreview = postContentPreview;
        event.reason = reason;
        return event;
    }
}

