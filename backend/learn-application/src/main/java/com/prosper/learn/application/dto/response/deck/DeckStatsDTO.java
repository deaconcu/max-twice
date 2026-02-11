package com.prosper.learn.application.dto.response.deck;

import lombok.Data;

/**
 * 卡片组统计信息响应DTO
 */
@Data
public class DeckStatsDTO {

    private Integer totalCardCount;

    private Integer newCardCount;

    private Integer reviewCardCount;

    private Integer learnedCardCount;

}