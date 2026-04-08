package com.prosper.learn.application.dto.response.roadmap;

import lombok.Data;

/**
 * 路线图简要 DTO
 *
 * 用途：极简路线图信息,用于学习进度查询
 * 使用场景：用户学习中心的路线图列表
 *
 * @author Claude
 * @since 2025-12-28
 */
@Data
public class RoadmapBriefDTO {

    /**
     * 路线图ID
     */
    private Long id;

    /**
     * 专业名称
     */
    private String roleName;

    /**
     * 专业图标
     */
    private String roleIcon;

    /**
     * 节点数量
     */
    private Integer nodeCount;
}
