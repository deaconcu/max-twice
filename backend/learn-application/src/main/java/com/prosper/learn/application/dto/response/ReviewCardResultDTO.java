package com.prosper.learn.application.dto.response;

import lombok.Data;

/**
 * 单次复习结果响应DTO
 */
@Data
public class ReviewCardResultDTO {

    private Long cardId;

    private Integer result;

    private Integer timeSpent;

}