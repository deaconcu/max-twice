package com.prosper.learn.dto.response.card;

import com.prosper.learn.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片（含创建者）DTO
 *
 * 用途：包含创建者信息的卡片
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CardWithCreatorDTO extends CardWithDeckDTO {

    private UserBriefDTO creator;
}
