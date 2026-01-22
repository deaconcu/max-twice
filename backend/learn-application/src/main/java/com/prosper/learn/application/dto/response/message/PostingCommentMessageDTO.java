package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

@Data
public class PostingCommentMessageDTO extends MessageDTO {

    NodeBriefDTO node;

    Long postingId;

    UserBriefDTO commenter;

}
