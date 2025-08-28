package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDO {

    private Long id;

    private String name;

    private String description;

    private Long creator;

    private Long rootNode;

    private Long parent;

    private String state; // 修改为 String 类型，支持 SUMMITTED, APPROVED, REJECTED

    private Integer mainCategory; // 新增主分类字段

    private Integer subCategory; // 新增子分类字段

    private String rejectedReason; // 拒绝原因，默认为空字符串

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
