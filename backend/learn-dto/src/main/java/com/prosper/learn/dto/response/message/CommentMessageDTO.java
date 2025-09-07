package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.NodeDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class CommentMessageDTO extends MessageDTO {

    NodeDTO node;

    UserDTO commenter;

    Long commentId;

}
