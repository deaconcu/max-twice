package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeSummaryDTO;
import lombok.Data;

@Data
public class CommentMessageDTO extends MessageDTO {

    NodeSummaryDTO node;

    UserBriefDTO commenter;

    Long commentId;

}
