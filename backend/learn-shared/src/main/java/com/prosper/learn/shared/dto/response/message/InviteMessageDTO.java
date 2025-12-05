package com.prosper.learn.shared.dto.response.message;

import com.prosper.learn.shared.dto.response.UserDTO;
import com.prosper.learn.shared.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class InviteMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    UserDTO inviter;
}
