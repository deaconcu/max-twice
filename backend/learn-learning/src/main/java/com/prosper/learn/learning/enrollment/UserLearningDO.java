package com.prosper.learn.learning.enrollment;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户学习记录统一表
 * 替代 user_course 和 user_roadmap
 *
 * 表名：user_learning
 * 索引：
 * - PRIMARY KEY (id)
 * - UNIQUE KEY (user_id, object_type, object_id)
 * - INDEX (user_id, object_type)
 */
@Data
public class UserLearningDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对象类型 (4=roadmap, 8=course)
     */
    private Byte objectType;

    /**
     * 对象ID
     */
    private Long objectId;

    /**
     * 父对象ID（仅 roadmap 使用）
     * - roadmap: parent_id = profession_id
     * - course: parent_id = null 或 0
     */
    private Long parentId;

    /**
     * 是否为课程根节点
     * 仅当 objectType=node 时有意义
     * 0 = 普通节点（READ页直接学习的节点）
     * 1 = 课程根节点（代表学习课程）
     */
    private Byte isRootNode;

    /**
     * 进度百分比 (0-10000，精度到万分位)
     */
    private Integer progressPercent;

    /**
     * 开始学习时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 课程ToC中的节点ID列表（JSON数组格式）
     * 仅 objectType=course 时使用
     * 用于快速查找节点完成时需要更新哪些课程进度
     * 格式: "[1,2,3,4,5]"
     *
     * 维护时机：用户选择ToC时更新
     * 使用场景：节点完成时，通过此字段反向查找受影响的课程
     */
    private String nodes;

    /**
     * 根据进度计算学习状态
     * 1 = 进行中 (0 < progress < 10000)
     * 2 = 已完成 (progress >= 10000)
     */
    public Byte getState() {
        if (progressPercent != null && progressPercent >= 10000) {
            return (byte) 2;  // COMPLETED
        } else {
            return (byte) 1;  // IN_PROGRESS
        }
    }
}
