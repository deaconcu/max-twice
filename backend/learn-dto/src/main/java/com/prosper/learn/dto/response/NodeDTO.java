package com.prosper.learn.dto.response;

import com.prosper.learn.dto.response.old.CourseDTOV4;
import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private CourseDTO course;

    private List<NodeDTO> children;

    private Long creatorId;

    private Integer commentCount;

    private Byte state;

    private String createdAt;

    private String updatedAt;

    private Boolean isCompleted;
}
