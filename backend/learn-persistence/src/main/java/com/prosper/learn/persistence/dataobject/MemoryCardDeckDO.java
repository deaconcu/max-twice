package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Integer upvoteCount;

    private Integer cardCount;

    private Double score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}