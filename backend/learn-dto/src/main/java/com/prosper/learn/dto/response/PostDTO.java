package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class PostDTO {

    private Long id;

    private String content;

    private Long nodeId;

    private NodeDTO node;

    private Long creatorId;

    private UserDTO creator;

    private Integer type;

    private Integer once;

    private Integer twice;

    private Integer helpful;

    private Integer commentCount;

    private Integer viewCount;

    private Integer state;

    private Double score;

    private String createdAt;

    private String updatedAt;

    private Integer voteType;
}