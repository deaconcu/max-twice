package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDO {

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

    private double score;

    private LocalDateTime cTime;
}
