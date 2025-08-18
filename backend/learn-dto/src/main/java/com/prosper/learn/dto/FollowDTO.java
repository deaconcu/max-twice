package com.prosper.learn.dto;

import lombok.Data;

@Data
public class FollowDTO {

    private int followerId;

    private int followeeId;

    private String createTime;
}
