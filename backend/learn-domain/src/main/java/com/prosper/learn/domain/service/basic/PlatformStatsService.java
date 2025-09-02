package com.prosper.learn.domain.service.basic;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.response.PlatformStatsDTO;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 平台统计服务
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformStatsService {
    
    private final CourseDataService courseDataService;
    private final ProfessionDataService professionDataService;
    private final RoadmapDataService roadmapDataService;
    private final NodeDataService nodeDataService;
    private final PostDataService postDataService;
    
    // ========== 常量定义 ==========
    public static final String CACHE_KEY = "stats";
    
    /**
     * 获取平台统计数据（带缓存）
     * 
     * @return 平台统计数据
     */
    @Cacheable(value = "platformStats", key = "'stats'", sync = true)
    public PlatformStatsDTO getPlatformStats() {
        log.info("开始计算平台统计数据");
        
        try {
            // 统计课程总数（只统计已发布的课程）
            Long courseCount = courseDataService.countActiveCourses();
            
            // 统计职业路径总数（只统计已发布的职业）
            Long careerPathCount = professionDataService.countActiveProfessions();
            
            // 统计学习路线图总数（只统计公开的路线图）
            Long roadmapCount = roadmapDataService.countPublicRoadmaps();
            
            // 统计知识节点总数（所有有效节点）
            Long knowledgeNodeCount = nodeDataService.countActiveNodes();
            
            // 统计文章总数（只统计已发布的文章）
            Long articleCount = postDataService.countActiveArticles();
            
            PlatformStatsDTO stats = new PlatformStatsDTO(
                courseCount, 
                careerPathCount, 
                roadmapCount, 
                knowledgeNodeCount,
                articleCount
            );
            
            log.info("平台统计数据计算完成: {}", stats);
            return stats;
            
        } catch (Exception e) {
            log.error("计算平台统计数据失败", e);
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
    }
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 最新的平台统计数据
     */
    @CacheEvict(value = "platformStats", key = "'stats'")
    public PlatformStatsDTO refreshPlatformStats() {
        log.info("刷新平台统计数据缓存");
        return getPlatformStats();
    }
}