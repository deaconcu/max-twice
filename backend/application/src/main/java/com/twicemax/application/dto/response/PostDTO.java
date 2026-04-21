package com.twicemax.application.dto.response;

import com.twicemax.application.dto.response.node.NodeWithCourseDTO;
import com.twicemax.application.dto.response.user.UserBriefDTO;
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

    private Integer twiceCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private Integer state;

    private Double score;

    private String createdAt;

    private String updatedAt;

    private Integer voteType;
}