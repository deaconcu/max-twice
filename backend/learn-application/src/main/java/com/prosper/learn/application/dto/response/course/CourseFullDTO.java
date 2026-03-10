package com.prosper.learn.application.dto.response.course;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程完整信息 DTO
 *
 * 用途：返回课程的完整信息，包含基础信息、统计数据和用户学习状态
 *
 * 使用场景：
 * - 课程详情页
 * - 课程列表（需要显示统计和进度）
 * - 用户学习中心
 * - 热门课程排行榜
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseFullDTO extends CourseSummaryDTO {

    /**
     * 学习人数
     * 说明：当前正在学习该课程的用户数量
     * 何时填充：从统计服务查询
     */
    private Integer learnerCount;

    /**
     * 收藏人数
     * 说明：收藏该课程的总用户数量
     * 何时填充：从统计服务查询
     */
    private Integer bookmarkCount;

    /**
     * 是否已收藏
     * 说明：当前用户是否已收藏该课程
     * 何时填充：动态查询当前用户与课程的收藏关系
     * 未登录时：null 或 false
     */
    private Boolean bookmarked;

    /**
     * 学习进度（万分位：0-10000）
     * 说明：课程完成进度，万分位精度
     * 何时填充：动态计算当前用户的学习进度
     * 前端转换：progress / 100 = 百分比（0-100）
     * 未登录时：null 或 0
     */
    private Integer progress;
}
