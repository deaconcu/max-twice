package com.prosper.learn.dto;

import lombok.Data;

@Data
public class UserDTOV2 {

    private Long id;

    private String name;

    private SubscriptionDTO[] subscriptions;

}
