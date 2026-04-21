package com.twicemax.application.dto.response.roadmap;

import com.twicemax.application.dto.response.role.RoleBriefDTO;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 路线图摘要 DTO
 *
 * 用途：基础路线图信息
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class RoadmapSummaryDTO {

    private Long id;

    private String content;

    private Long roleId;

    private RoleBriefDTO role;

    private String description;

    private Byte state;

    private Integer likeCount;

    private Integer commentCount;

    private Integer learnerCount;

    private Integer nodeCount;

    private Long creatorId;

    private UserBriefDTO creator;

    /**
     * 内容是否可用
     * true: 内容可用
     * false: 内容不可用（已删除、已屏蔽、已拒绝等）
     * null: 默认可用
     */
    private Boolean available;

    private String updatedAt;

    private String createdAt;
}
