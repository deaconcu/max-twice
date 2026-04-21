package com.twicemax.interaction.comment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDO {

    private Long id;

    private String content;

    private Integer objectType;

    private Long objectId;

    private Long replyToCommentId;

    private Long creatorId;

    private Long toUserId;

    private Byte state;

    private String reason;  // 拒绝/封禁原因

    private Double score;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;  // 软删除时间
}
