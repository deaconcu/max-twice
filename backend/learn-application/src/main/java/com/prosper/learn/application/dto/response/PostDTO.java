package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.node.NodeWithCourseDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

@Data
public class PostDTO {

    private Long id;

    private String content;

    private Long nodeId;

    private NodeWithCourseDTO node;

    private Long creatorId;

    private UserBriefDTO creator;

    private Integer type;

    private Integer twice;

    private Integer helpful;

    private Integer commentCount;

    private Integer viewCount;

    private Integer state;

    private Double score;

    private String createdAt;

    private String updatedAt;

    private Integer voteType;
}