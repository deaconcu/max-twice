package com.prosper.learn.domain.event.user.relationship;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 用户关注事件
 * 当用户关注另一个用户时触发
 */
@Data
@AllArgsConstructor
public class UserFollowedEvent {

    /** 关注者ID */
    private Long followerId;

    /** 被关注者ID */
    private Long followeeId;
}