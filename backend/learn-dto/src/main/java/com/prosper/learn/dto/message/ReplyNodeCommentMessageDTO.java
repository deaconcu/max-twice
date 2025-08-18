package com.prosper.learn.dto.message;

import com.prosper.learn.dto.*;
import lombok.Data;

@Data
public class ReplyNodeCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    int commentId;

    UserDTOV4 commenter;
}
