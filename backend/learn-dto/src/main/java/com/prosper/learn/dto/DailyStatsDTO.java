package com.prosper.learn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsDTO {
    
    private LocalDate date;
    
    private Long views;
    
    private Long twice;
    
    private Long helpful;
    
    private Long comments;
}