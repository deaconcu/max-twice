package com.prosper.learn.dto.response;

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

    private UserDTO creator;

    private UserCardSrsStateDTO srsState;

}