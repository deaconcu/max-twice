package com.prosper.learn.application.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private Long id;

    private String content;

    private Integer objectType;

    private Long objectId;

    private Integer replyCount;

    private Long replyToCommentId;

    private Long creatorId;

    private String creatorName;

    private Long toUserId;

    private String toUserName;

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
