package com.prosper.learn.shared.dto.response.old;

import com.prosper.learn.shared.dto.response.UserDTO;
import lombok.Data;

@Data
public class PostDTOV1 {

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

    private Integer state;

    private Double score;

    private String createdAt;

    private String updatedAt;

    private Integer voteType;
}