package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowDO {

    private Long followeeId;

    private Long followerId;

    private LocalDateTime createTime;
}
