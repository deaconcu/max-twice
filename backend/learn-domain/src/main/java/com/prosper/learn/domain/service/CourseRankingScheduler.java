package com.prosper.learn.domain.service;

import com.prosper.learn.persistence.mapper.CourseMapper;
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

    /**
     * 每小时同步一次课程统计数据到Redis
     * cron表达式: 每小时的第0分钟执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncCourseStats() {
        log.info("开始同步课程统计数据到Redis...");
        
        try {
            // 获取所有课程的收藏数据
            String subscriptionSql = """
                SELECT course_id, COUNT(*) as subscription_count 
                FROM user_subscription 
                GROUP BY course_id
                """;
            
            List<Map<String, Object>> subscriptionData = jdbcTemplate.queryForList(subscriptionSql);
            
            // 获取所有课程的学习数据（正在学习和已完成的）
            String learningSql = """
                SELECT course_id, COUNT(*) as learning_count 
                FROM user_course 
                WHERE status IN ('IN_PROGRESS', 'COMPLETED') 
                GROUP BY course_id
                """;
            
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(learningSql);
            
            // 同步收藏数据
            for (Map<String, Object> row : subscriptionData) {
                Integer courseId = (Integer) row.get("course_id");
                Long count = ((Number) row.get("subscription_count")).longValue();
                
                if (courseId != null && count != null) {
                    // 这里直接设置，而不是增量更新，因为是全量同步
                    String subscriptionKey = "course:subscription:" + courseId;
                    courseRankingService.initializeCourseStats(courseId, count, 0);
                }
            }
            
            // 同步学习数据
            for (Map<String, Object> row : learningData) {
                Integer courseId = (Integer) row.get("course_id");
                Long count = ((Number) row.get("learning_count")).longValue();
                
                if (courseId != null && count != null) {
                    // 获取已有的收藏数
                    CourseRankingService.CourseStats stats = courseRankingService.getCourseStats(courseId);
                    courseRankingService.initializeCourseStats(courseId, stats.getSubscriptionCount(), count);
                }
            }
            
            log.info("课程统计数据同步完成，收藏数据: {} 条，学习数据: {} 条", 
                    subscriptionData.size(), learningData.size());
            
        } catch (Exception e) {
            log.error("同步课程统计数据失败", e);
        }
    }

    /**
     * 应用启动时执行一次初始化
     */
    @Scheduled(initialDelay = 5000) // 启动5秒后执行
    public void initializeCourseStats() {
        log.info("初始化课程统计数据...");
        syncCourseStats();
    }

    /**
     * 手动触发同步（可以通过管理接口调用）
     */
    public void manualSync() {
        log.info("手动触发课程统计数据同步...");
        syncCourseStats();
    }
}