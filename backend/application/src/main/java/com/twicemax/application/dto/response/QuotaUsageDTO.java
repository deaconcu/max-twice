package com.twicemax.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 配额使用情况DTO
 */
@Data
@AllArgsConstructor
public class QuotaUsageDTO {

    /**
     * 每分钟已使用
     */
    private int minuteUsed;

    /**
     * 每分钟限制
     */
    private int minuteLimit;

    /**
     * 每小时已使用
     */
    private int hourUsed;

    /**
     * 每小时限制
     */
    private int hourLimit;

    /**
     * 每天已使用
     */
    private int dailyUsed;

    /**
     * 每天限制
     */
    private int dailyLimit;
}
