package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.old.NodeDTOV1;
import com.prosper.learn.application.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class ReplyPostingCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    Long postingId;

    Long commentId;

    UserDTOV4 commenter;

}
