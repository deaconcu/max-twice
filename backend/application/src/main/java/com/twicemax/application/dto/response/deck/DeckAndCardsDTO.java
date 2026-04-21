package com.twicemax.application.dto.response.deck;

import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 卡片组详情响应DTO（含卡片列表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeckAndCardsDTO extends DeckFullDTO {

    private List<CardWithSrsDTO> cards;
}
