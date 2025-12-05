package com.prosper.learn.shared.dto.response.deck;

import com.prosper.learn.shared.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片组（含创建者）DTO
 *
 * 用途：包含创建者信息的卡片组
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeckWithCreatorDTO extends DeckSummaryDTO {

    private UserBriefDTO creator;
}
