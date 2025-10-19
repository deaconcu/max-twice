package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDO {

    private Long id;

    private String content;

    private Integer objectType;

    private Long objectId;

    private Integer replyCount;

    private Long replyToCommentId;

    private Long creatorId;

    private Long toUserId;

    private Integer upvoteCount;

    private Byte state;

    private Double score;

    private LocalDateTime createdAt;
}
