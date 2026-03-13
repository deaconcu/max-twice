package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
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
