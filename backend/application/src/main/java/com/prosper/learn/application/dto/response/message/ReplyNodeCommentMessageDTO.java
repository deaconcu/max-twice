package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReplyNodeCommentMessageDTO extends MessageDTO {

    NodeBriefDTO node;

    Long commentId;

    UserBriefDTO commenter;
}
