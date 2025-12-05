package com.prosper.learn.shared.dto.response.old;

import com.prosper.learn.shared.dto.response.SubscriptionDTO;
import lombok.Data;

@Data
public class UserDTOV2 {

    private Long id;

    private String name;

    private SubscriptionDTO[] subscriptions;

}
