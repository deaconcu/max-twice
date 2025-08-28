package com.prosper.learn.dto;

import lombok.Data;

@Data
public class CommentDTOV1 {

    private int id;

    private String content;

    private int type;

    private int objectId;

    private int replyCount;

    private int replyTo;

    private int fromUser;

    private int toUser;

    private int upvoteCount;

    private int state;

    private String createdAt;

}
