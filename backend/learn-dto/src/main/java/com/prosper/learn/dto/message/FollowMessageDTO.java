package com.prosper.learn.dto.message;

import com.prosper.learn.dto.NodeDTOV1;
import com.prosper.learn.dto.UserDTOV4;
import lombok.Data;

@Data
public class FollowMessageDTO extends MessageDTO {

    NodeDTOV1 node;

    Long postingId;

    Integer voteType;

    UserDTOV4 follower;
}
