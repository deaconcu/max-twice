package com.prosper.learn.dto.message;

import com.prosper.learn.dto.*;
import lombok.Data;

@Data
public class ReplyPostingCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    Long postingId;

    Long commentId;

    UserDTOV4 commenter;

}
