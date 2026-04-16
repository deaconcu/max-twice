package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeSummaryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpvoteMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    Long objectId;

    Integer objectType;

    Integer voteType;

    UserBriefDTO upvoter;
}
