package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class CommentDTOV1 {

    private Long id;

    private String content;

    private Integer type;

    private Long objectId;

    private Integer replyCount;

    private Long replyTo;

    private Long fromUser;

    private Long toUser;

    private Integer upvoteCount;

    private Integer state;

    private String createdAt;

}
