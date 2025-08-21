package com.prosper.learn.domain.service;

import com.prosper.learn.dto.PlatformStatsDTO;
import com.prosper.learn.persistence.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 平台统计服务
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Service
public class PlatformStatsService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlatformStatsService.class);
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private ProfessionMapper professionMapper;
    
    @Autowired
    private RoadmapMapper roadmapMapper;
    
    @Autowired
    private NodeMapper nodeMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    /**
     * 获取平台统计数据（带缓存）
     * 
     * @return 平台统计数据
     */
    @Cacheable(value = "platformStats", key = "'stats'", sync = true)
    public PlatformStatsDTO getPlatformStats() {
        logger.info("开始计算平台统计数据");
        
        try {
            // 统计课程总数（只统计已发布的课程）
            Long courseCount = courseMapper.countActiveCourses();
            
            // 统计职业路径总数（只统计已发布的职业）
            Long careerPathCount = professionMapper.countActiveProfessions();
            
            // 统计学习路线图总数（只统计公开的路线图）
            Long roadmapCount = roadmapMapper.countPublicRoadmaps();
            
            // 统计知识节点总数（所有有效节点）
            Long knowledgeNodeCount = nodeMapper.countActiveNodes();
            
            // 统计文章总数（只统计已发布的文章）
            Long articleCount = postMapper.countActiveArticles();
            
            PlatformStatsDTO stats = new PlatformStatsDTO(
                courseCount, 
                careerPathCount, 
                roadmapCount, 
                knowledgeNodeCount,
                articleCount
            );
            
            logger.info("平台统计数据计算完成: {}", stats);
            return stats;
            
        } catch (Exception e) {
            logger.error("计算平台统计数据失败", e);
            throw new RuntimeException("获取平台统计数据失败", e);
        }
    }
    
    /**
     * 刷新平台统计数据缓存
     * 
     * @return 最新的平台统计数据
     */
    @CacheEvict(value = "platformStats", key = "'stats'")
    public PlatformStatsDTO refreshPlatformStats() {
        logger.info("刷新平台统计数据缓存");
        return getPlatformStats();
    }
}