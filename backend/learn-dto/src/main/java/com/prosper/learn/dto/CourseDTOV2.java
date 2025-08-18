package com.prosper.learn.dto;

import lombok.Data;

@Data
public class CourseDTOV2 {

    private int id;

    private String name;

    private String description;

    private int mainCategory; // 主分类 ID

    private int subCategory; // 子分类 ID
}
