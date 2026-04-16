package com.prosper.learn.memory.card;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryCardVersionDO {

    private Long id;

    private Long cardId;

    private Integer version;

    private Long creatorId;

    private String front;

    private String back;

    private String contentHash;

    private Boolean isActive;

    private LocalDateTime createdAt;

}