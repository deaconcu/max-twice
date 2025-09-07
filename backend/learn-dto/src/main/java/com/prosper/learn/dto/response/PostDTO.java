package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.old.NodeDTOV0;
import com.prosper.learn.dto.response.old.UserDTOV1;
import lombok.Data;

@Data
public class PostDTO {

    private Long id;

    private String content;

    private Long nodeId;

    private NodeDTOV0 node;

    private Long creatorId;

    private UserDTO creator;

    private Integer type;

    private Integer once;

    private Integer twice;

    private Integer helpful;

    private Integer commentCount;

    private Integer views;

    private Integer state;

    private Double score;

    private String createdAt;

    private String updatedAt;

    private Integer voteType;
}