package com.prosper.learn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    
    private Long userId;
    
    private String period; // "today", "7days", "15days", "30days", "1year", "all"
    
    private String startDate; // 格式: yyyy-MM-dd
    
    private String endDate; // 格式: yyyy-MM-dd
    
    private Long totalViews;        // 总阅读量
    
    private Long totalTwice;        // max twice 总数
    
    private Long totalHelpful;      // 有帮助总数
    
    private Long totalComments;     // 评论总数
    
    // 每日明细（用于图表展示）
    private List<DailyStatsDTO> dailyStats;
    
    public static UserStatsDTO empty() {
        return UserStatsDTO.builder()
            .totalViews(0L)
            .totalTwice(0L)
            .totalHelpful(0L)
            .totalComments(0L)
            .build();
    }
}