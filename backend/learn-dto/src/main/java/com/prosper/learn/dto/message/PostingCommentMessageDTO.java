package com.prosper.learn.dto.message;

import com.prosper.learn.dto.*;
import lombok.Data;

@Data
public class PostingCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    int postingId;

    UserDTOV4 commenter;

}
