package com.prosper.learn.analytics.ranking.scheduler;

import com.prosper.learn.analytics.ranking.service.RoleRankingDomainService;
import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
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
public class RoleRankingScheduler {

    private final RoleRankingDomainService roleRankingService;
    private final JdbcTemplate jdbcTemplate;
    private final SystemProperties systemProperties;

    // 不变常量 - 状态相关
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_APPROVED = "APPROVED";
    
    // 不变常量 - 日志消息
    private static final String LOG_START_SYNC = "开始同步角色统计数据到Redis...";
    private static final String LOG_SYNC_COMPLETE = "角色统计数据同步完成，更新了 {} 个角色的学习数据";
    private static final String LOG_INITIALIZE = "初始化角色统计数据...";
    private static final String LOG_MANUAL_SYNC = "手动触发角色统计数据同步...";

    // ========== 私有辅助方法 ==========

    /**
     * 验证数据库查询结果
     */
    private void validateQueryResult(List<Map<String, Object>> data) {
        if (data == null) {
            throw StatusCode.DATABASE_ERROR.exception();
        }
    }

    /**
     * 验证和获取角色ID
     */
    private Long validateAndGetRoleId(Map<String, Object> row) {
        Number roleIdNum = (Number) row.get("role_id");
        if (roleIdNum == null) {
            return null;
        }
        return roleIdNum.longValue();
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
            SELECT p.id as role_id, COUNT(ur.id) as learning_count 
            FROM role p
            LEFT JOIN roadmap r ON p.id = r.role_id
            LEFT JOIN user_roadmap ur ON r.id = ur.roadmap_id AND ur.state = ?
            WHERE p.state = ?
            GROUP BY p.id
            """;
    }

    /**
     * 处理角色学习数据
     */
    private int processRoleLearningData(List<Map<String, Object>> learningData) {
        int updatedCount = 0;
        for (Map<String, Object> row : learningData) {
            Long roleId = validateAndGetRoleId(row);
            Long learningCount = validateAndGetCount(row);
            
            if (roleId != null) {
                try {
                    roleRankingService.initializeRoleStats(roleId, learningCount);
                    updatedCount++;
                } catch (Exception e) {
                    log.warn("初始化角色 {} 统计数据失败: {}", roleId, e.getMessage());
                }
            }
        }
        return updatedCount;
    }

    // ========== 公共业务方法 ==========

    /**
     * 每小时同步一次角色统计数据到Redis（为每种语言执行）
     */
    @Scheduled(cron = "0 15 * * * ?")
    public void syncRoleStats() {
        if (!systemProperties.getScheduler().isEnableRoleRankingSync()) {
            return;
        }

        DataSourceContextHolder.forEachLanguage(lang -> {
            log.info("[{}] {}", lang, LOG_START_SYNC);
            try {
                syncRoleStatsInternal(lang);
            } catch (Exception e) {
                log.error("[{}] 同步角色统计数据失败", lang, e);
            }
        });
    }

    /**
     * 内部同步方法（单语言）
     */
    private void syncRoleStatsInternal(String lang) {
        try {
            // 先清空Redis中的统计数据
            roleRankingService.clearAllStats();

            // 获取所有角色的学习数据（通过roadmap关联）
            String sql = getLearningDataSql();
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(
                sql, STATUS_IN_PROGRESS, STATUS_APPROVED);

            validateQueryResult(learningData);

            // 同步学习数据到Redis
            int updatedCount = processRoleLearningData(learningData);

            log.info("[{}] {}", lang, String.format(LOG_SYNC_COMPLETE.replace("{}", "%d"), updatedCount));

        } catch (Exception e) {
            log.error("[{}] 同步角色统计数据失败", lang, e);
            throw StatusCode.SCHEDULER_DATA_SYNC_FAILED.exception(e);
        }
    }

    /**
     * 应用启动时执行一次初始化
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initializeRoleStats() {
        if (!systemProperties.getScheduler().isEnableRoleRankingStartupInit()) {
            return;
        }

        try {
            log.info(LOG_INITIALIZE);
            syncRoleStats();
        } catch (Exception e) {
            log.error("初始化角色统计数据失败", e);
            throw StatusCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }

    /**
     * 手动触发同步（可以通过管理接口调用）
     */
    public void manualSync() {
        try {
            log.info(LOG_MANUAL_SYNC);
            syncRoleStats();
        } catch (Exception e) {
            log.error("手动同步失败", e);
            throw StatusCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }
}