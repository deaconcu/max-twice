package com.prosper.learn.domain.service;

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

    /**
     * 每小时同步一次课程统计数据到Redis
     * cron表达式: 每小时的第0分钟执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void syncCourseStats() {
        log.info("开始同步课程统计数据到Redis...");
        
        try {
            // 先清空Redis中的统计数据
            courseRankingService.clearAllStats();
            
            // 分页处理用户收藏数据
            int pageSize = 1000;
            int offset = 0;
            int subscriptionCount = 0;
            
            while (true) {
                List<UserProfileDO> userProfiles = userProfileMapper.getSubscriptionDataByPage(offset, pageSize);
                
                if (userProfiles.isEmpty()) {
                    break; // 没有更多数据，结束循环
                }
                
                // 处理当前页的数据
                for (UserProfileDO profile : userProfiles) {
                    String subscription = profile.getSubscription();
                    if (subscription != null && !subscription.trim().isEmpty()) {
                        // 解析收藏的课程ID列表
                        String[] courseIds = subscription.split(",");
                        for (String courseIdStr : courseIds) {
                            try {
                                int courseId = Integer.parseInt(courseIdStr.trim());
                                // 增加该课程的收藏数
                                courseRankingService.incrementSubscription(courseId);
                                subscriptionCount++;
                            } catch (NumberFormatException e) {
                                // 忽略无效的课程ID
                            }
                        }
                    }
                }
                
                offset += pageSize;
            }
            
            // 获取所有课程的学习数据（正在学习和已完成的）
            String learningSql = """
                SELECT course_id, COUNT(*) as learning_count 
                FROM user_course 
                WHERE status IN ('IN_PROGRESS', 'COMPLETED') 
                GROUP BY course_id
                """;
            
            List<Map<String, Object>> learningData = jdbcTemplate.queryForList(learningSql);
            
            // 同步学习数据到Redis
            for (Map<String, Object> row : learningData) {
                Number courseIdNum = (Number) row.get("course_id");
                Long count = ((Number) row.get("learning_count")).longValue();
                
                if (courseIdNum != null && count != null) {
                    int courseId = courseIdNum.intValue();
                    // 增加学习数统计
                    for (int i = 0; i < count; i++) {
                        courseRankingService.incrementLearning(courseId);
                    }
                }
            }
            
            log.info("课程统计数据同步完成，收藏数据: {} 条，学习数据: {} 条", 
                    subscriptionCount, learningData.size());
            
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