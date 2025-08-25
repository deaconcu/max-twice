package com.prosper.learn.dto;

import lombok.Data;

@Data
public class PostDTOV2 {

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

    private int views;

    private int state;

    private double score;

    private String cTime;

    private String uTime;

    private int voteType;
}