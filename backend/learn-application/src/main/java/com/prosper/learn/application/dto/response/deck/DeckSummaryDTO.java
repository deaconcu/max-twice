package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import lombok.Data;

/**
 * 卡片组摘要 DTO
 *
 * 用途：基础卡片组信息
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class DeckSummaryDTO {

    private Long id;

    private Long postId;

    private Long nodeId;

    private Long courseId;

    private String title;

    private String description;

    private Integer state;

    private String updatedAt;

    private String createdAt;

    private Integer upvoteCount;

    private Integer cardCount;

    private CourseBriefDTO course;

    private NodeBriefDTO node;
}
