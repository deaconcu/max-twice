package com.prosper.learn.application.dto.response.deck;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片组（含点赞状态）DTO
 *
 * 用途：包含创建者和点赞状态的卡片组
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeckWithVoteDTO extends DeckWithCreatorDTO {

    /**
     * 当前用户是否已点赞
     */
    private Boolean hasLiked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean bookmarked;
}
