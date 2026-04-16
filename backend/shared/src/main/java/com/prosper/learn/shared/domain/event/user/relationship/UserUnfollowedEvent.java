package com.prosper.learn.shared.domain.event.user.relationship;

import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 用户取消关注事件
 * 当用户取消关注另一个用户时触发
 */
@Data
@AllArgsConstructor
public class UserUnfollowedEvent {

    /** 取消关注者ID */
    private Long followerId;

    /** 被取消关注者ID */
    private Long followeeId;
}