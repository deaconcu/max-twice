package com.prosper.learn.dto.response;

import lombok.Data;

/**
 * 记忆卡片组响应DTO
 */
@Data
public class MemoryCardDeckDTO {

    private Long id;

    private Long sourcePostId;

    private UserDTO creator;

    private String title;

    private String description;

    private Integer state;

    private String updatedAt;

    private String createdAt;

    private Integer upvoteCount;

    private Integer cardCount;

}