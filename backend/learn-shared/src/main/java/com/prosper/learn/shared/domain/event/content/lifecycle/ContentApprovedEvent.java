package com.prosper.learn.shared.domain.event.content.lifecycle;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;

import java.util.List;

/**
 * 内容审核通过事件
 * 当内容审核通过时触发（SUBMITTED → PUBLISHED）
 * 用于发送审核通过通知、更新统计等
 */
@Data
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

    /** 帖子ID（memory_card_deck 类型使用）*/
    private Long postId;

    /** 帖子类型（post 类型使用，1=CONTENTS, 2=ARTICLE）*/
    private Integer postType;

    /** 目录型帖子引用的节点ID列表（post 类型且 postType=CONTENTS 时使用）*/
    private List<Long> referencedNodeIds;

    /** 被评论对象的类型（comment 类型使用）*/
    private ContentType commentTargetType;

    /** 被评论对象的ID（comment 类型使用）*/
    private Long commentTargetId;

    // ========== 各类型专用构造函数 ==========

    /** Profession 类型构造函数 */
    public static ContentApprovedEvent forProfession(Long creatorId, Long professionId, String professionName) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = professionId;
        event.contentType = ContentType.profession;
        event.professionName = professionName;
        return event;
    }

    /** Course 类型构造函数 */
    public static ContentApprovedEvent forCourse(Long creatorId, Long courseId, String courseName) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = courseId;
        event.contentType = ContentType.course;
        event.courseName = courseName;
        return event;
    }

    /** Node 类型构造函数 */
    public static ContentApprovedEvent forNode(Long creatorId, Long nodeId, String nodeName, Long courseId, String courseName) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = nodeId;
        event.contentType = ContentType.node;
        event.nodeName = nodeName;
        event.courseId = courseId;
        event.courseName = courseName;
        return event;
    }

    /** Post 类型构造函数 */
    public static ContentApprovedEvent forPost(Long creatorId, Long postId, String postPreview, Long nodeId, String nodeName, String courseName, Integer postType) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.postContentPreview = postPreview;
        event.nodeId = nodeId;
        event.nodeName = nodeName;
        event.courseName = courseName;
        event.postType = postType;
        return event;
    }

    /** Index 类型 Post 构造函数（目录型帖子，只包含必要字段）*/
    public static ContentApprovedEvent forIndexPost(Long creatorId, Long postId, Long nodeId, List<Long> referencedNodeIds) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = postId;
        event.contentType = ContentType.post;
        event.nodeId = nodeId;
        event.postType = 1; // PostType.CONTENTS
        event.referencedNodeIds = referencedNodeIds;
        return event;
    }

    /** Roadmap 类型构造函数 */
    public static ContentApprovedEvent forRoadmap(Long creatorId, Long roadmapId, Long professionId, String professionName) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = roadmapId;
        event.contentType = ContentType.roadmap;
        event.professionId = professionId;
        event.professionName = professionName;
        return event;
    }

    /** MemoryCardDeck 类型构造函数 */
    public static ContentApprovedEvent forMemoryCardDeck(Long creatorId, Long deckId, Long postId,
                                                         String postContentPreview, Long nodeId) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = deckId;
        event.contentType = ContentType.memory_card_deck;
        event.postId = postId;
        event.postContentPreview = postContentPreview;
        event.nodeId = nodeId;
        return event;
    }

    /** Comment 类型构造函数 */
    public static ContentApprovedEvent forComment(Long creatorId, Long commentId, ContentType commentTargetType, Long commentTargetId) {
        ContentApprovedEvent event = new ContentApprovedEvent();
        event.creatorId = creatorId;
        event.contentId = commentId;
        event.contentType = ContentType.comment;
        event.commentTargetType = commentTargetType;
        event.commentTargetId = commentTargetId;
        return event;
    }
}
