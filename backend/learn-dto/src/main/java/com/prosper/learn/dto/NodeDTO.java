package com.prosper.learn.dto;

import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {

    private Integer id;

    private String name;

    private String description;

    private int courseId;

    private CourseDTOV4 course;

    private int root;

    private List<NodeDTO> children;

    private int creator;

    private int commentCount;

    private String cTime;

    private String uTime;
}
