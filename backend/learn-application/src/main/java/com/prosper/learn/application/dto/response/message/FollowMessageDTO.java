package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class FollowMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    Long postingId;

    Integer voteType;

    UserBriefDTO follower;
}
