package com.prosper.learn.shared.dto.response.message;

import com.prosper.learn.shared.dto.response.UserDTO;
import com.prosper.learn.shared.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class UpvoteMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    Long objectId;

    Integer objectType;

    Integer voteType;

    UserDTO upvoter;
}
