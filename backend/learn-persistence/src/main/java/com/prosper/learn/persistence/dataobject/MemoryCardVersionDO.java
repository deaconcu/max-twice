package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.math.BigDecimal;
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

    private Integer isActive;

    private LocalDateTime createdAt;

}