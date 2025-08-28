package com.prosper.learn.dto;

import lombok.Data;

@Data
public class FollowDTO {

    private Long followerId;

    private Long followeeId;

    private String createdAt;
}
