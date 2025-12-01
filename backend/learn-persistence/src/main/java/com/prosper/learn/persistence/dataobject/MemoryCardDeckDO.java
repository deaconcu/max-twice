package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MemoryCardDeckDO {

    private Long id;

    private Long postId;

    private Long nodeId;  // 卡片组所属的节点ID，冗余字段便于查询

    private Long creatorId;

    private String title;

    private String description;

    private Integer version;

    private Byte state;

    private String reason;  // 拒绝或屏蔽的原因

    //private Long auditorId;

    //private LocalDateTime auditedAt;

    private LocalDateTime updatedAt;

    private Integer cardCount;

    private Double score;

    private LocalDateTime createdAt;

}