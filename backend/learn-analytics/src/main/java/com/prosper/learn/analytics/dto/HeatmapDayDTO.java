package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 热力图单日数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDayDTO {

    /** 日期，格式: yyyy-MM-dd */
    private String date;

    /** 完成节点数 */
    private Integer completedNodes;

    /** 复习卡片数 */
    private Integer reviewedCards;

    /** 活动值 = completedNodes * 10 + reviewedCards */
    private Integer activityValue;
}
