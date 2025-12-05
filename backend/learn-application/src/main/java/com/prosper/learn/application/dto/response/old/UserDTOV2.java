package com.prosper.learn.application.dto.response.old;

import com.prosper.learn.application.dto.response.SubscriptionDTO;
import lombok.Data;

@Data
public class UserDTOV2 {

    private Long id;

    private String name;

    private SubscriptionDTO[] subscriptions;

}
