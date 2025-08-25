package com.prosper.learn.dto;

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
    
    private Long views;
    
    private Long twice;
    
    private Long helpful;
    
    private Long comments;
}