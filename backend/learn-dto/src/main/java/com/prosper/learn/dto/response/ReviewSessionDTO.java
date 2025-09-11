package com.prosper.learn.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 复习会话响应DTO
 */
@Data
public class ReviewSessionDTO {

    private String startTime;

    private String endTime;

    private Integer totalCards;

    private Integer reviewedCards;

    private Integer correctAnswers;

    private List<ReviewCardResultDTO> results;

}