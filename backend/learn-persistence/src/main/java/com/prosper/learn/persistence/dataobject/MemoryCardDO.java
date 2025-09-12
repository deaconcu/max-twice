package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryCardDO {

    private Long id;

    private Long deckId;

    private Long creatorId;

    private Long currentVersionId;

    private Byte state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}