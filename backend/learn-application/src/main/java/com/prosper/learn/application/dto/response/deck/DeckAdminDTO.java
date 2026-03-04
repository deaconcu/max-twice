package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

import java.util.List;

/**
 * 卡片组管理 DTO
 *
 * 用途：管理后台使用的卡片组信息
 *
 * 使用场景：
 * - 管理后台记忆卡片审核列表
 * - 需要显示拒绝/封禁原因的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class DeckAdminDTO {

    private Long id;

    private Long postId;

    private Long nodeId;

    private Long courseId;

    private String title;

    private String description;

    private Integer state;

    /**
     * 拒绝/封禁原因
     */
    private String reason;

    private String updatedAt;

    private String createdAt;

    private Integer likeCount;

    private Integer cardCount;

    /**
     * 排序分数
     */
    private Double score;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 收藏数
     */
    private Integer bookmarkCount;

    /**
     * 被拒次数
     */
    private Integer rejectCount;

    private CourseBriefDTO course;

    private NodeBriefDTO node;

    private Long creatorId;

    private UserBriefDTO creator;

    private List<CardWithSrsDTO> cards;
}
