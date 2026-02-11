package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 卡片组详情响应DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeckDetailDTO extends DeckWithCreatorDTO {

    private List<CardWithSrsDTO> cards;

    //private DeckStatsDTO stats;

}