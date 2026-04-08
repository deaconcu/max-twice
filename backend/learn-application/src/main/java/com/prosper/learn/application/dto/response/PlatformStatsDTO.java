package com.prosper.learn.application.dto.response;

import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.Data;

/**
 * 平台统计数据DTO
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Data
public class PlatformStatsDTO {
    
    /**
     * 课程总数
     */
    private Long courseCount;
    
    /**
     * 角色路径总数
     */
    private Long careerPathCount;
    
    /**
     * 学习路线图总数
     */
    private Long roadmapCount;
    
    /**
     * 知识节点总数
     */
    private Long knowledgeNodeCount;
    
    /**
     * 文章数量
     */
    private Long articleCount;
    
    /**
     * 最后更新时间
     */
    private String lastUpdated;
    
    public PlatformStatsDTO() {
        this.lastUpdated = TimeZoneUtil.formatDateTime(TimeZoneUtil.nowDateTime());
    }
    
    public PlatformStatsDTO(Long courseCount, Long careerPathCount, Long roadmapCount, Long knowledgeNodeCount, Long articleCount) {
        this.courseCount = courseCount;
        this.careerPathCount = careerPathCount;
        this.roadmapCount = roadmapCount;
        this.knowledgeNodeCount = knowledgeNodeCount;
        this.articleCount = articleCount;
        this.lastUpdated = TimeZoneUtil.formatDateTime(TimeZoneUtil.nowDateTime());
    }
}