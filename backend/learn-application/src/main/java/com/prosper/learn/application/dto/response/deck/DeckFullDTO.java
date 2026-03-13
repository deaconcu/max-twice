package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 卡片组完整 DTO
 *
 * 用途：包含所有字段的卡片组信息
 */
@Data
public class DeckFullDTO {

    private Long id;

    private Long postId;

    private Long nodeId;

    private Long courseId;

    private String description;

    private Integer state;

    private String updatedAt;

    private String createdAt;

    private Integer likeCount;

    private Integer cardCount;

    private CourseBriefDTO course;

    private NodeBriefDTO node;

    private UserBriefDTO creator;

    /** 当前用户是否已点赞 */
    private Boolean hasLiked;

    /** 当前用户是否已收藏 */
    private Boolean bookmarked;

    /** 第一张卡片的问题（用于列表展示） */
    private String firstCardQuestion;
}
