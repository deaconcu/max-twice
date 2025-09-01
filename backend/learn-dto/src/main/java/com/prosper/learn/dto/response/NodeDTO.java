package com.prosper.learn.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private CourseDTOV4 course;

    private Long root;

    private List<NodeDTO> children;

    private Long creator;

    private Integer commentCount;

    private String createdAt;

    private String updatedAt;
}
