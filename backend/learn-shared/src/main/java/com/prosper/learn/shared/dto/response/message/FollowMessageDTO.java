package com.prosper.learn.shared.dto.response.message;

import com.prosper.learn.shared.dto.response.UserDTO;
import com.prosper.learn.shared.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class FollowMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    Long postingId;

    Integer voteType;

    UserDTO follower;
}
