package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.old.NodeDTOV1;
import com.prosper.learn.application.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class NodeCommentMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    UserDTOV4 commenter;

}
