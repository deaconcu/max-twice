package com.prosper.learn.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 卡片组详情响应DTO
 */
@Data
public class DeckDetailDTO extends MemoryCardDeckDTO {

    private List<MemoryCardViewDTO> cards;

    //private DeckStatsDTO stats;

}