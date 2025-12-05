package com.prosper.learn.application.dto.response;

import lombok.Data;

/**
 * 卡片组统计信息响应DTO
 */
@Data
public class DeckStatsDTO {

    private Integer totalCards;

    private Integer newCards;

    private Integer reviewCards;

    private Integer learnedCards;

}