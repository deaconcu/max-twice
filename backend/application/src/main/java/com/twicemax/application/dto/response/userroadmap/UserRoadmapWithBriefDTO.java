package com.twicemax.application.dto.response.userroadmap;

import com.twicemax.application.dto.response.roadmap.RoadmapBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户路线图（含简要路线图信息）DTO
 *
 * 用途：包含路线图简要信息的学习记录
 * 使用场景：个人学习中心路线图列表
 *
 * @author Claude
 * @since 2025-12-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoadmapWithBriefDTO extends UserRoadmapSummaryDTO {

    /**
     * 路线图简要信息
     * 说明：包含路线图的 id、专业名称和节点数量
     */
    private RoadmapBriefDTO roadmap;
}
