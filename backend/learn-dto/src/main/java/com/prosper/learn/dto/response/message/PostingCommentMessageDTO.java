package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.old.NodeDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class PostingCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    Long postingId;

    UserDTOV4 commenter;

}
