package com.prosper.learn.shared.dto.response;

import lombok.Data;

@Data
public class FollowDTO {

    private Long followerId;

    private Long followeeId;

    private String createdAt;
}
