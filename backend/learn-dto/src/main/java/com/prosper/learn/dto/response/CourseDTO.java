package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class CourseDTO {

    private Long id;

    private String name;

    private String description;

    private Long creator;

    private Long rootNode;

    private Long parentId;

    private String state; // 修改为 String 类型，支持 SUMMITTED, APPROVED, REJECTED

    private Integer mainCategory; // 新增主分类字段

    private Integer subCategory; // 新增子分类字段

    private String rejectedReason; // 拒绝原因，默认为空字符串

    private String createdAt;

    private String updatedAt;
}