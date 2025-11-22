package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class CommentMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    UserDTO commenter;

    Long commentId;

}
