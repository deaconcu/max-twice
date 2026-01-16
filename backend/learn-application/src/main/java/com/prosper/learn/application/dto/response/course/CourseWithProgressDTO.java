package com.prosper.learn.application.dto.response.course;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带学习进度的课程 DTO
 *
 * 用途：用户个人学习中心，显示课程详情和个人学习数据
 *
 * 使用场景：
 * - 用户学习中心（我的课程列表）
 * - 课程学习页面（显示当前进度）
 * - 任何需要展示用户学习进度的场景
 *
 * 替代关系：
 * - 替代原 V5（V4 + subscribed + progress）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseWithProgressDTO extends CourseDetailDTO {

    /**
     * 是否已收藏
     * 说明：当前用户是否已订阅该课程
     * 何时填充：动态查询当前用户与课程的订阅关系填充
     */
    private Boolean bookmarked;

    /**
     * 学习进度
     * 说明：课程完成百分比（0-100）
     * 何时填充：动态计算当前用户的学习进度填充
     * 计算规则：已完成节点数 / 总节点数 * 100
     */
    private Integer progress;
}
