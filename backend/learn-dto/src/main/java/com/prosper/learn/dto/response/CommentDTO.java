package com.prosper.learn.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private Long id;

    private String content;

    private Integer type;

    private Long objectId;

    private Integer replyCount;

    private Long replyTo;

    private Long fromUser;

    private Long toUser;

    private Integer upvoteCount;

    private Boolean upvoted;

    private String createdAt;

    private List<CommentDTO> children;

    public void addChild(CommentDTO commentDTO) {
        if (children == null) children = new ArrayList<>();
        children.add(commentDTO);
    }
}
