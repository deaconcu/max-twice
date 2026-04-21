package com.twicemax.analytics.stats.mapper;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStatsYearlyDO {

    private Long userId;

    private String stats;  // JSON格式存储统计数据，格式：{"9-5": [0,1,3,5], "9-9": [0,5,0,0]} 数组为[views, twice, helpful, comments]

    private Integer statYear;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}