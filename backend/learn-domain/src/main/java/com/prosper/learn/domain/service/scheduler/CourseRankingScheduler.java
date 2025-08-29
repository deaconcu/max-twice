package com.prosper.learn.domain.service.scheduler;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
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
public class CourseRankingScheduler {

    private final CourseRankingService courseRankingService;
    private final JdbcTemplate jdbcTemplate;
    private final UserProfileMapper userProfileMapper;
    private final SystemProperties systemProperties;

    // 不变常量 - 状态相关
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    
    // 不变常量 - 日志消息
    private static final String LOG_START_SYNC = "开始同步课程统计数据到Redis...";
    private static final String LOG_SYNC_COMPLETE = "课程统计数据同步完成，收藏数据: {} 条，学习数据: {} 条";
    private static final String LOG_INITIALIZE = "初始化课程统计数据...";
    private static final String LOG_MANUAL_SYNC = "手动触发课程统计数据同步...";

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
     * 验证和获取课程ID
     */
    private Integer validateAndGetCourseId(Map<String, Object> row) {
        Number courseIdNum = (Number) row.get("course_id");
        if (courseIdNum == null) {
            return null;
        }
        return courseIdNum.intValue();
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
            SELECT course_id, COUNT(*) as learning_count 
            FROM user_course 
            WHERE status IN (?, ?) 
            GROUP BY course_id
            """;
    }

    /**
     * 处理用户收藏数据
     */
    private int processUserSubscriptions() {
        int subscriptionCount = 0;
        int offset = 0;
        
        while (true) {
            List<UserProfileDO> userProfiles = userProfileMapper.getSubscriptionDataByPage(
                offset, systemProperties.getScheduler().getSyncBatchSize());
            
            if (userProfiles.isEmpty()) {
                break;
            }
            
            for (UserProfileDO profile : userProfiles) {
                String subscription = profile.getSubscription();
                if (subscription != null && !subscription.trim().isEmpty()) {
                    String[] courseIds = subscription.split(",");
                    for (String courseIdStr : courseIds) {
                        try {
                            int courseId = Integer.parseInt(courseIdStr.trim());
                            courseRankingService.incrementSubscription(courseId);
                            subscriptionCount++;
                        } catch (NumberFormatException e) {
                            log.warn("无效的课程ID: {}", courseIdStr);
                        }
                    }
                }
            }
            
            offset += systemProperties.getScheduler().getSyncBatchSize();
        }
        
        return subscriptionCount;
    }

    /**
     * 处理课程学习数据
     */
    private int processCourseLearningData(List<Map<String, Object>> learningData) {
        int processedCount = 0;
        for (Map<String, Object> row : learningData) {
            Integer courseId = validateAndGetCourseId(row);
            Long count = validateAndGetCount(row);
            
            if (courseId != null && count != null) {
                try {
                    for (int i = 0; i < count; i++) {
                        courseRankingService.incrementLearning(courseId);
                    }
                    processedCount++;
                } catch (Exception e) {
                    log.warn("处理课程 {} 学习数据失败: {}", courseId, e.getMessage());
                }
            }
        }
        return processedCount;
    }

    // ========== 公共业务方法 ==========

    /**
     * 每小时同步一次课程统计数据到Redis
     */
    @Scheduled(cron = "#{systemProperties.scheduler.courseRankingSyncCron}")
    public void syncCourseStats() {
        if (!systemProperties.getCourseRanking().isEnableRankingUpdate()) {
            return;
        }
        
        log.info(LOG_START_SYNC);
        
        try {
            // 先清空Redis中的统计数据
            courseRankingService.clearAllStats();
            
            // 处理用户收藏数据
            int subscriptionCount = processUserSubscriptions();
            
            // 获取所有课程的学习数据
            String sql = getLearningDataSql();
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(
                sql, STATUS_IN_PROGRESS, STATUS_COMPLETED);
            
            validateQueryResult(learningData);
            
            // 同步学习数据到Redis
            int learningDataCount = processCourseLearningData(learningData);
            
            log.info(LOG_SYNC_COMPLETE, subscriptionCount, learningDataCount);
            
        } catch (Exception e) {
            log.error("同步课程统计数据失败", e);
            throw ErrorCode.SCHEDULER_DATA_SYNC_FAILED.exception(e);
        }
    }

    /**
     * 应用启动时执行一次初始化
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initializeCourseStats() {
        if (!systemProperties.getScheduler().isEnableStartupInitialization()) {
            return;
        }
        
        try {
            log.info(LOG_INITIALIZE);
            syncCourseStats();
        } catch (Exception e) {
            log.error("初始化课程统计数据失败", e);
            throw ErrorCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }

    /**
     * 手动触发同步（可以通过管理接口调用）
     */
    public void manualSync() {
        try {
            log.info(LOG_MANUAL_SYNC);
            syncCourseStats();
        } catch (Exception e) {
            log.error("手动同步失败", e);
            throw ErrorCode.SCHEDULER_TASK_FAILED.exception(e);
        }
    }
}