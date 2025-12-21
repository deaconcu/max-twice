package com.prosper.learn.application.dto.response.course;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程摘要（含统计和进度）DTO
 *
 * 用途：课程详情页面，显示课程基本信息、统计数据和用户学习状态
 *
 * 使用场景：
 * - 课程详情页（CourseDetailPage.vue）
 * - 需要同时展示课程信息、热度统计和用户学习状态的场景
 *
 * 字段说明：
 * - 继承自 CourseSummaryDTO：id, name, description, mainCategory, subCategory
 * - 统计字段：learnerCount, subscriptionCount
 * - 用户字段：subscribed, progress
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseSummaryWithStatsAndProgressDTO extends CourseSummaryDTO {

    /**
     * 学习人数
     * 说明：当前正在学习该课程的用户数量
     * 何时填充：从 Redis 排行榜服务动态查询并填充
     */
    private Integer learnerCount;

    /**
     * 订阅人数（收藏人数）
     * 说明：订阅（收藏）该课程的总用户数量
     * 何时填充：从 Redis 排行榜服务动态查询并填充
     */
    private Integer subscriptionCount;

    /**
     * 是否已订阅（收藏）
     * 说明：当前用户是否已订阅该课程
     * 何时填充：动态查询当前用户与课程的订阅关系填充
     * 未登录时：null 或 false
     */
    private Boolean subscribed;

    /**
     * 学习进度
     * 说明：课程完成百分比（0-100）
     * 何时填充：动态计算当前用户的学习进度填充
     * 计算规则：已完成节点数 / 总节点数 * 100
     * 未登录时：null 或 0
     */
    private Integer progress;
}
