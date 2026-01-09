package com.prosper.learn.user.profile;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileDO {

    private Long userId;

    private String subscription;

    private String roadmapPin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UserProfileDO() {}

    public UserProfileDO(Long userId, String subscription) {
        this.userId = userId;
        this.subscription = subscription;
        this.roadmapPin = "";
    }
}
