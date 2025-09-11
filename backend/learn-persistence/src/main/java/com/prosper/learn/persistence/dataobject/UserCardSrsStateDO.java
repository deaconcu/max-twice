package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCardSrsStateDO {

    private Long id;

    private Long userId;

    private Long cardId;

    private Integer deckVersion;

    private Long cardVersionId;

    private LocalDateTime reviewDueAt;

    private LocalDateTime lastReviewedAt;

    private Integer intervalDays;

    private BigDecimal easeFactor;

    private Integer repetitions;

    private Integer lapseCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}