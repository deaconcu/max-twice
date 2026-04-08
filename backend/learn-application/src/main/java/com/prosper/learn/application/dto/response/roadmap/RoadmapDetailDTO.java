package com.prosper.learn.application.dto.response.roadmap;

import com.prosper.learn.application.dto.response.role.RoleBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 路线图详情 DTO
 *
 * 用途：包含创建者和专业信息的路线图
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoadmapDetailDTO extends RoadmapSummaryDTO {

    private UserBriefDTO creator;

    private RoleBriefDTO role;
}
