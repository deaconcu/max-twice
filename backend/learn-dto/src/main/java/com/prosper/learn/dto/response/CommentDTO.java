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

    private Long replyToCommentId;

    private Long fromUserId;

    private Long toUserId;

    private Integer upvoteCount;

    private Integer state;

    private Double score;

    private Boolean upvoted;

    private String createdAt;

    private List<CommentDTO> children;

    public void addChild(CommentDTO commentDTO) {
        if (children == null) children = new ArrayList<>();
        children.add(commentDTO);
    }
}
