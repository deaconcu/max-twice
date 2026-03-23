package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 热力图数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapDataDTO {

    private Long userId;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 期间完成节点总数 */
    private Integer totalCompletedNodes;

    /** 期间复习卡片总数 */
    private Integer totalReviewedCards;

    /** 活跃天数 */
    private Integer activeDays;

    /** 每日数据 */
    private List<HeatmapDayDTO> dailyData;

    public static HeatmapDataDTO empty() {
        return HeatmapDataDTO.builder()
            .totalCompletedNodes(0)
            .totalReviewedCards(0)
            .activeDays(0)
            .dailyData(List.of())
            .build();
    }
}
