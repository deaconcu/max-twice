package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class UserDTOV2 {

    private Long id;

    private String name;

    private SubscriptionDTO[] subscriptions;

}
