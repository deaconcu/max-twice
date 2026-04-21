package com.twicemax.analytics.stats.mapper;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentStatsYearlyDO {

    // 复合主键字段
    private Integer objectType; // 使用 ObjectType 枚举值（post=1, roadmap=4）

    private Long objectId;

    private Integer statYear;

    private String stats;  // JSON格式存储点赞数据，格式：{"1-1":{"twice": 3, "helpful": 2, "views": 100, "comments": 8}, ...}

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
