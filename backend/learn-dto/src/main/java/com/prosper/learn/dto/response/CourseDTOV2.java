package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class CourseDTOV2 {

    private Long id;

    private String name;

    private String description;

    private Integer mainCategory; // 主分类 ID

    private Integer subCategory; // 子分类 ID
}
