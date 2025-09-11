package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryCardDeckDO {

    private Long id;

    private Long sourcePostId;

    private Long creatorId;

    private String title;

    private String description;

    private Integer version;

    private Integer state;

    private Long auditorId;

    private LocalDateTime auditedAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    private Integer upvoteCount;

    private Integer cardCount;

    private Double score;

    private LocalDateTime createdAt;

}