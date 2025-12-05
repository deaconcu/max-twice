package com.prosper.learn.shared.dto.response.card;

import com.prosper.learn.shared.dto.response.deck.DeckSummaryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片（含卡片组）DTO
 *
 * 用途：包含所属卡片组信息的卡片
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CardWithDeckDTO extends CardContentDTO {

    private DeckSummaryDTO deck;
}
