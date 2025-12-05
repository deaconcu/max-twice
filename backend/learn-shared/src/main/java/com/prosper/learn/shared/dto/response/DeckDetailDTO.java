package com.prosper.learn.shared.dto.response;

import com.prosper.learn.shared.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.shared.dto.response.deck.DeckWithCreatorDTO;
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