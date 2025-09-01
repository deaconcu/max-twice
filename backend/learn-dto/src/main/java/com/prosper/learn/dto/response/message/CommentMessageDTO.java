package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.NodeDTOV1;
import com.prosper.learn.dto.response.UserDTOV4;
import lombok.Data;

@Data
public class CommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    UserDTOV4 commenter;

    Long commentId;

}
