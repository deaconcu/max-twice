package com.prosper.learn.dto;

import lombok.Data;

@Data
public class SubcourseDTO {

    private int id;

    private String name;

    private String description;

    private int courseId;

    private int creator;

    private int state;

    private String createdAt;

    private String updatedAt;
}