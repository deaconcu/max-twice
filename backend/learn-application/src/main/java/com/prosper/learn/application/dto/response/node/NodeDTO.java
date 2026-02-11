package com.prosper.learn.application.dto.response.node;

import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private Byte isCourseRoot;

    private List<NodeDTO> children;

    private Long creatorId;

    private Integer commentCount;

    private Byte state;

    private String createdAt;

    private String updatedAt;

    private Boolean isCompleted;
}
