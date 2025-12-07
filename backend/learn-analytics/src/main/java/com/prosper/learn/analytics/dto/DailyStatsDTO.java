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
    
    private Integer views;
    
    private Integer twice;
    
    private Integer likes;
    
    private Integer comments;
}