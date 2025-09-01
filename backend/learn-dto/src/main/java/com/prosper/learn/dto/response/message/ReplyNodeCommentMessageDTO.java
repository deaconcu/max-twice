package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.NodeDTOV1;
import com.prosper.learn.dto.response.UserDTOV4;
import lombok.Data;

@Data
public class ReplyNodeCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    Long commentId;

    UserDTOV4 commenter;
}
