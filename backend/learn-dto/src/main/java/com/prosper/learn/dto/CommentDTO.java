package com.prosper.learn.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private int id;

    private String content;

    private int type;

    private int objectId;

    private int replyCount;

    private int replyTo;

    private int fromUser;

    private int toUser;

    private int upvoteCount;

    private int upvoted;

    private String cTime;

    private List<CommentDTO> children;

    public void addChild(CommentDTO commentDTO) {
        if (children == null) children = new ArrayList<>();
        children.add(commentDTO);
    }
}
