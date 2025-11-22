package com.prosper.learn.dto.response.card;

import com.prosper.learn.dto.response.UserCardSrsDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片（含SRS学习状态）DTO
 *
 * 用途：包含用户学习状态的卡片
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CardWithSrsDTO extends CardWithCreatorDTO {

    private UserCardSrsDTO srsState;

    private Boolean hasDeckUpdate;

    private Boolean hasCardUpdate;
}
