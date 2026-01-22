package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

@Data
public class ReplyPostingCommentMessageDTO extends MessageDTO {

    NodeBriefDTO node;

    Long postingId;

    Long commentId;

    UserBriefDTO commenter;

}
