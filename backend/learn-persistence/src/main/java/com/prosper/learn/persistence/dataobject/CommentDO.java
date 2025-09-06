package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDO {

    private Long id;

    private String content;

    private Integer type;

    private Long objectId;

    private Integer replyCount;

    private Long replyToUserId;

    private Long fromUserId;

    private Long toUserId;

    private Integer upvoteCount;

    private Integer state;

    private Double score;

    private LocalDateTime createdAt;
}
