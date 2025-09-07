package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.NodeDTOV1;
import com.prosper.learn.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class FollowMessageDTO extends MessageDTO {

    NodeDTO node;

    Long postingId;

    Integer voteType;

    UserDTO follower;
}
