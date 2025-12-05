package com.prosper.learn.shared.dto.response.roadmap;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路线图（含用户状态）DTO
 *
 * 用途：包含用户点赞、置顶、学习状态的路线图
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoadmapWithStatusDTO extends RoadmapDetailDTO {

    private Boolean upvoted;

    private Boolean pinned;

    private Boolean learning;
}
