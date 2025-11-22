package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 记忆卡片视图响应DTO
 */
@Data
public class MemoryCardViewDTO {

    private Long id;

    private String front;

    private String back;

    private MemoryCardDeckDTO deck;

    private UserBriefDTO creator;

    private UserCardSrsDTO srsState;

    private Boolean hasDeckUpdate;  // 所在deck是否有更新
    private Boolean hasCardUpdate;  // 卡片内容本身是否有更新

}