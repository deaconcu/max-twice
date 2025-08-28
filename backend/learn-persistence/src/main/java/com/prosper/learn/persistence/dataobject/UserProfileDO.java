package com.prosper.learn.persistence.dataobject;

import lombok.Data;

@Data
public class UserProfileDO {

    private Long id;

    private String subscription;

    private String roadmapPin;

    public UserProfileDO() {}

    public UserProfileDO(Long id, String subscription) {
        this.id = id;
        this.subscription = subscription;
    }
}
