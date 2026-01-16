package com.prosper.learn.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsDTO {
    
    private String date; // 格式: yyyy-MM-dd
    
    private Integer viewCount;
    
    private Integer twiceCount;
    
    private Integer likeCount;
    
    private Integer commentCount;
}