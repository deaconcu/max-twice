package com.twicemax.application.dto.response.userroadmap;

import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户路线图（含路线图详细信息）DTO
 *
 * 用途：包含路线图详细信息的学习记录
 * 使用场景：个人学习中心、路线图详情页等需要展示路线图信息的场景
 *
 * 替代：原 UserRoadmapDTO V1
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRoadmapWithDetailDTO extends UserRoadmapSummaryDTO {

    /**
     * 路线图详细信息
     * 说明：包含路线图的完整信息（id, content, role, creator等）
     */
    private RoadmapDetailDTO roadmap;
}
