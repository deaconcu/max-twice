package com.prosper.learn.user.profile;

import lombok.Data;

@Data
public class UserProfileDO {

    private Long userId;

    private String subscription;

    private String roadmapPin;

    public UserProfileDO() {}

    public UserProfileDO(Long userId, String subscription) {
        this.userId = userId;
        this.subscription = subscription;
        this.roadmapPin = "";
    }
}
