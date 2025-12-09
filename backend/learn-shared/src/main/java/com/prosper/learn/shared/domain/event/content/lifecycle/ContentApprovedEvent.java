package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容审核通过事件
 * 当内容审核通过时触发（SUBMITTED → PUBLISHED）
 * 用于发送审核通过通知、更新统计等
 */
@Data
@AllArgsConstructor
public class ContentApprovedEvent {

    /** 创建者ID */
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

    // ========== 各类型专用构造函数 ==========

    /** Profession 类型构造函数 */
    public static ContentApprovedEvent forProfession(Long creatorId, Long professionId, String professionName) {
        return new ContentApprovedEvent(
            creatorId, professionId, ContentType.profession,
            null, null, null, null, null,
            professionName, null, null, null
        );
    }

    /** Course 类型构造函数 */
    public static ContentApprovedEvent forCourse(Long creatorId, Long courseId, String courseName) {
        return new ContentApprovedEvent(
            creatorId, courseId, ContentType.course,
            courseName, null, null, null, null,
            null, null, null, null
        );
    }

    /** Node 类型构造函数 */
    public static ContentApprovedEvent forNode(Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName) {
        return new ContentApprovedEvent(
            creatorId, nodeId, ContentType.node,
            courseName, nodeName, courseId, null, null,
            null, null, null, null
        );
    }

    /** Post 类型构造函数 */
    public static ContentApprovedEvent forPost(Long creatorId, Long postId, String postPreview, Long nodeId, String nodeName, String courseName) {
        return new ContentApprovedEvent(
            creatorId, postId, ContentType.post,
            courseName, nodeName, null, postPreview, nodeId,
            null, null, null, null
        );
    }

    /** Roadmap 类型构造函数 */
    public static ContentApprovedEvent forRoadmap(Long creatorId, Long roadmapId, Long professionId, String professionName) {
        return new ContentApprovedEvent(
            creatorId, roadmapId, ContentType.roadmap,
            null, null, null, null, null,
            professionName, professionId, null, null
        );
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentApprovedEvent forMemoryCardDeck(Long creatorId, Long deckId, String deckTitle, Long postId, String postContentPreview) {
        return new ContentApprovedEvent(
            creatorId, deckId, ContentType.memory_card_deck,
            null, null, null, postContentPreview, null,
            null, null, deckTitle, postId
        );
    }
}
