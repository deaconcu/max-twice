package com.prosper.learn.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfessionRankingScheduler {

    private final ProfessionRankingService professionRankingService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 每小时同步一次职业统计数据到Redis
     */
    @Scheduled(cron = "0 15 * * * ?") // 每小时第15分钟执行
    public void syncProfessionStats() {
        log.info("开始同步职业统计数据到Redis...");
        
        try {
            // 先清空Redis中的统计数据
            professionRankingService.clearAllStats();
            
            // 获取所有职业的学习数据（通过roadmap关联）
            String learningSql = """
                SELECT p.id as profession_id, COUNT(ur.id) as learning_count 
                FROM profession p
                LEFT JOIN roadmap r ON p.id = r.profession_id
                LEFT JOIN user_roadmap ur ON r.id = ur.roadmap_id AND ur.status = 'IN_PROGRESS'
                WHERE p.state = 'APPROVED'
                GROUP BY p.id
                """;
            
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(learningSql);
            
            // 同步学习数据到Redis
            int updatedCount = 0;
            for (Map<String, Object> row : learningData) {
                Number professionIdNum = (Number) row.get("profession_id");
                Number countNum = (Number) row.get("learning_count");
                
                if (professionIdNum != null && countNum != null) {
                    int professionId = professionIdNum.intValue();
                    long learningCount = countNum.longValue();
                    
                    // 初始化职业统计数据
                    professionRankingService.initializeProfessionStats(professionId, learningCount);
                    updatedCount++;
                }
            }
            
            log.info("职业统计数据同步完成，更新了 {} 个职业的学习数据", updatedCount);
            
        } catch (Exception e) {
            log.error("同步职业统计数据失败", e);
        }
    }

    /**
     * 应用启动时执行一次初始化
     */
    @Scheduled(initialDelay = 10000) // 启动10秒后执行
    public void initializeProfessionStats() {
        log.info("初始化职业统计数据...");
        syncProfessionStats();
    }

    /**
     * 手动触发同步（可以通过管理接口调用）
     */
    public void manualSync() {
        log.info("手动触发职业统计数据同步...");
        syncProfessionStats();
    }
}