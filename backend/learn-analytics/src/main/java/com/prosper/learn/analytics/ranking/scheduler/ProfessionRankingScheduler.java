package com.prosper.learn.analytics.ranking.scheduler;

import com.prosper.learn.analytics.ranking.service.ProfessionRankingDomainService;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
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

    private final ProfessionRankingDomainService professionRankingService;
    private final JdbcTemplate jdbcTemplate;
    private final SystemProperties systemProperties;

    // 不变常量 - 状态相关
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_APPROVED = "APPROVED";
    
    // 不变常量 - 日志消息
    private static final String LOG_START_SYNC = "开始同步职业统计数据到Redis...";
    private static final String LOG_SYNC_COMPLETE = "职业统计数据同步完成，更新了 {} 个职业的学习数据";
    private static final String LOG_INITIALIZE = "初始化职业统计数据...";
    private static final String LOG_MANUAL_SYNC = "手动触发职业统计数据同步...";

    // ========== 私有辅助方法 ==========

    /**
     * 验证数据库查询结果
     */
    private void validateQueryResult(List<Map<String, Object>> data) {
        if (data == null) {
            throw ErrorCode.DATABASE_ERROR.exception();
        }
    }

    /**
     * 验证和获取职业ID
     */
    private Long validateAndGetProfessionId(Map<String, Object> row) {
        Number professionIdNum = (Number) row.get("profession_id");
        if (professionIdNum == null) {
            return null;
        }
        return professionIdNum.longValue();
    }

    /**
     * 验证和获取计数
     */
    private Long validateAndGetCount(Map<String, Object> row) {
        Number countNum = (Number) row.get("learning_count");
        if (countNum == null) {
            return 0L;
        }
        return countNum.longValue();
    }

    /**
     * 获取学习数据SQL
     */
    private String getLearningDataSql() {
        return """
            SELECT p.id as profession_id, COUNT(ur.id) as learning_count 
            FROM profession p
            LEFT JOIN roadmap r ON p.id = r.profession_id
            LEFT JOIN user_roadmap ur ON r.id = ur.roadmap_id AND ur.state = ?
            WHERE p.state = ?
            GROUP BY p.id
            """;
    }

    /**
     * 处理职业学习数据
     */
    private int processProfessionLearningData(List<Map<String, Object>> learningData) {
        int updatedCount = 0;
        for (Map<String, Object> row : learningData) {
            Long professionId = validateAndGetProfessionId(row);
            Long learningCount = validateAndGetCount(row);
            
            if (professionId != null) {
                try {
                    professionRankingService.initializeProfessionStats(professionId, learningCount);
                    updatedCount++;
                } catch (Exception e) {
                    log.warn("初始化职业 {} 统计数据失败: {}", professionId, e.getMessage());
                }
            }
        }
        return updatedCount;
    }

    // ========== 公共业务方法 ==========

    /**
     * 每小时同步一次职业统计数据到Redis
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void syncProfessionStats() {
        if (!systemProperties.getScheduler().isEnableProfessionRankingSync()) {
            return;
        }
        
        log.info(LOG_START_SYNC);
        
        try {
            // 先清空Redis中的统计数据
            professionRankingService.clearAllStats();
            
            // 获取所有职业的学习数据（通过roadmap关联）
            String sql = getLearningDataSql();
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(
                sql, STATUS_IN_PROGRESS, STATUS_APPROVED);
            
            validateQueryResult(learningData);
            
            // 同步学习数据到Redis
            int updatedCount = processProfessionLearningData(learningData);
            
            log.info(LOG_SYNC_COMPLETE, updatedCount);
            
        } catch (Exception e) {
            log.error("同步职业统计数据失败", e);
            throw ErrorCode.SCHEDULER_DATA_SYNC_FAILED.exception(e);
        }
    }

    /**
     * 应用启动时执行一次初始化
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initializeProfessionStats() {
        if (!systemProperties.getScheduler().isEnableStartupInitialization()) {
            return;
        }
        
        try {
            log.info(LOG_INITIALIZE);
            syncProfessionStats();
        } catch (Exception e) {
            log.error("初始化职业统计数据失败", e);
            throw ErrorCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }

    /**
     * 手动触发同步（可以通过管理接口调用）
     */
    public void manualSync() {
        try {
            log.info(LOG_MANUAL_SYNC);
            syncProfessionStats();
        } catch (Exception e) {
            log.error("手动同步失败", e);
            throw ErrorCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }
}