package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowDO {

    private int followeeId;

    private int followerId;

    private LocalDateTime createTime;
}
