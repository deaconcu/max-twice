package com.prosper.learn.interaction.follow;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowDO {

    private Long id;

    private Long followeeId;

    private Long followerId;

    private LocalDateTime createdAt;
}
