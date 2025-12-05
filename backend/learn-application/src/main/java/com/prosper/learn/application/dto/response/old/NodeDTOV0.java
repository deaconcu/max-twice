package com.prosper.learn.application.dto.response.old;

import lombok.Data;

import java.util.List;

@Data
public class NodeDTOV0 {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private CourseDTOV4 course;

    private List<NodeDTOV0> children;

    private Long creatorId;

    private Integer commentCount;

    private String createdAt;

    private String updatedAt;
}
