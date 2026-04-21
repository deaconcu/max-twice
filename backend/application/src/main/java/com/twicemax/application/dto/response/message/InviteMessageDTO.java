package com.twicemax.application.dto.response.message;

import com.twicemax.application.dto.response.user.UserBriefDTO;
import com.twicemax.application.dto.response.node.NodeSummaryDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InviteMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    UserBriefDTO inviter;
}
