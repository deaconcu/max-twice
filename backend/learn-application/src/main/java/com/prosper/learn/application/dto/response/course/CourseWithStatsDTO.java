package com.prosper.learn.application.dto.response.course;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带统计信息的课程 DTO
 *
 * 用途：热门课程排行榜，显示课程的受欢迎程度
 *
 * 使用场景：
 * - 热门课程排行榜
 * - 课程推荐列表
 * - 任何需要展示课程统计数据的场景
 *
 * 替代关系：
 * - 替代原 V6（dto + learnerCount + subscriptionCount）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseWithStatsDTO extends CourseSummaryDTO {

    /**
     * 学习人数
     * 说明：当前正在学习该课程的用户数量
     * 何时填充：从 Redis 排行榜服务动态查询并填充
     * 数据源：CourseRankingService.getCourseStats()
     */
    private Integer learnerCount;

    /**
     * 订阅人数（收藏人数）
     * 说明：订阅（收藏）该课程的总用户数量
     * 何时填充：从 Redis 排行榜服务动态查询并填充
     * 数据源：CourseRankingService.getCourseStats()
     */
    private Integer subscriptionCount;
}
