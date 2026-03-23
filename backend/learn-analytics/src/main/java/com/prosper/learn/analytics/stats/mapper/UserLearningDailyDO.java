package com.prosper.learn.analytics.stats.mapper;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日学习统计数据对象
 *
 * 记录用户每天的学习行为：完成节点数、复习卡片数
 * 用于热力图展示和连续学习天数计算
 */
@Data
public class UserLearningDailyDO {

    private Long userId;

    private LocalDate statDate;

    /** 当日完成节点数 */
    private Integer completedNodes;

    /** 当日复习卡片数 */
    private Integer reviewedCards;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
