package com.prosper.learn.dto;

import lombok.Data;

@Data
public class PostDTO {

    private Integer id;

    private String content;

    private int nodeId;

    private NodeDTO node;

    private int creatorId;

    private UserDTOV1 creator;

    private int type;

    private int once;

    private int twice;

    private int helpful;

    private int commentCount;

    private int state;

    private double score;

    private String createdAt;

    private String updatedAt;

    private int voteType;
}