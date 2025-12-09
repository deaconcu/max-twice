package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容审核拒绝事件
 * 当内容审核不通过时触发（SUBMITTED → REJECTED）
 * 用于发送拒绝通知，告知用户拒绝原因
 */
@Data
@AllArgsConstructor
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

    /** 职业名称（profession/roadmap 类型使用）*/
    private String professionName;

    /** 职业ID（roadmap 类型使用）*/
    private Long professionId;

    /** 卡片组标题（memory_card_deck 类型使用）*/
    private String deckTitle;

    /** 帖子ID（memory_card_deck 类型使用）*/
    private Long postId;

    /** 拒绝原因 */
    private String reason;

    // ========== 各类型专用构造函数 ==========

    /** Profession 类型构造函数 */
    public static ContentRejectedEvent forProfession(Long creatorId, Long professionId, String professionName, String reason) {
        return new ContentRejectedEvent(
            creatorId, professionId, ContentType.profession,
            null, null, null, null, null,
            professionName, null, null, null,
            reason
        );
    }

    /** Course 类型构造函数 */
    public static ContentRejectedEvent forCourse(Long creatorId, Long courseId, String courseName, String reason) {
        return new ContentRejectedEvent(
            creatorId, courseId, ContentType.course,
            courseName, null, null, null, null,
            null, null, null, null,
            reason
        );
    }

    /** Node 类型构造函数 */
    public static ContentRejectedEvent forNode(Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName, String reason) {
        return new ContentRejectedEvent(
            creatorId, nodeId, ContentType.node,
            courseName, nodeName, courseId, null, null,
            null, null, null, null,
            reason
        );
    }

    /** Post 类型构造函数 */
    public static ContentRejectedEvent forPost(Long creatorId, Long postId, String postPreview, Long nodeId, String nodeName, String courseName, String reason) {
        return new ContentRejectedEvent(
            creatorId, postId, ContentType.post,
            courseName, nodeName, null, postPreview, nodeId,
            null, null, null, null,
            reason
        );
    }

    /** Roadmap 类型构造函数 */
    public static ContentRejectedEvent forRoadmap(Long creatorId, Long roadmapId, Long professionId, String professionName, String reason) {
        return new ContentRejectedEvent(
            creatorId, roadmapId, ContentType.roadmap,
            null, null, null, null, null,
            professionName, professionId, null, null,
            reason
        );
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentRejectedEvent forMemoryCardDeck(Long creatorId, Long deckId, String deckTitle, Long postId, String postContentPreview, String reason) {
        return new ContentRejectedEvent(
            creatorId, deckId, ContentType.memory_card_deck,
            null, null, null, postContentPreview, null,
            null, null, deckTitle, postId,
            reason
        );
    }
}
