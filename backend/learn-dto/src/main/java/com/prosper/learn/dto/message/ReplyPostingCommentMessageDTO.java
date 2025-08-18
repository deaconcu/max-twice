package com.prosper.learn.dto.message;

import com.prosper.learn.dto.*;
import lombok.Data;

@Data
public class ReplyPostingCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    int postingId;

    int commentId;

    UserDTOV4 commenter;

}
