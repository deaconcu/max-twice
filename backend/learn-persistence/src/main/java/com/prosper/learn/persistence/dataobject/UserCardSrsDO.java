package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCardSrsDO {

    private Long id;

    private Long userId;

    private Long cardId;

    private Long nodeId;  // 新增：记忆卡片所属的节点ID

    private Long deckId;  // 冗余字段：记忆卡片组ID，用于快速过滤被屏蔽的卡片组

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