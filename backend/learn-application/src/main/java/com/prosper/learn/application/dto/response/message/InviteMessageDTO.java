package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.UserDTO;
import com.prosper.learn.application.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class InviteMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    UserDTO inviter;
}
