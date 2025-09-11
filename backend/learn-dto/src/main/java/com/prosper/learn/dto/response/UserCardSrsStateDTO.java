package com.prosper.learn.dto.response;

import lombok.Data;

/**
 * 用户卡片SRS状态响应DTO
 */
@Data
public class UserCardSrsStateDTO {

    private Long id;

    private String reviewDueAt;

    private String lastReviewedAt;

    private Integer intervalDays;

    private Integer repetitions;

    private Integer lapseCount;

}