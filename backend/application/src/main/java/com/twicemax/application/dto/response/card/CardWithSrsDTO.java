package com.twicemax.application.dto.response.card;

import com.twicemax.application.dto.response.UserCardSrsDTO;
import com.twicemax.application.dto.response.deck.DeckBriefDTO;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import com.twicemax.shared.domain.Enums;
import lombok.Data;

/**
 * 卡片（含SRS学习状态）DTO
 *
 * 用途：包含用户学习状态的卡片
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CardWithSrsDTO {

    // 基础字段（原 CardContentDTO）
    private Long id;

    private String front;

    private String back;

    /**
     * 卡片状态
     * @see Enums.ContentState
     */
    private Byte state;

    // 卡片组信息
    private DeckBriefDTO deck;

    // 创建者信息（原 CardWithCreatorDTO）
    private UserBriefDTO creator;

    // SRS 学习状态
    private UserCardSrsDTO srsState;

    private Boolean hasDeckUpdate;

    private Boolean hasCardUpdate;
}
