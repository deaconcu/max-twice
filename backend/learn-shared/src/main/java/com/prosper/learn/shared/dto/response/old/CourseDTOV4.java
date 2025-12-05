package com.prosper.learn.shared.dto.response.old;

import lombok.Data;

@Data
public class CourseDTOV4 {

    private Long id;

    private String name;

    private String description;

    private Long creatorId;

    private Long rootNodeId;

    private CourseDTOV3 parentCourse;

    private String state; // 修改为 String 类型，支持 SUMMITTED, APPROVED, REJECTED

    private Integer mainCategory; // 新增主分类字段

    private Integer subCategory; // 新增子分类字段

    private String reason; // 拒绝原因，默认为空字符串

    private String createdAt;

    private String updatedAt;

    // 新增：热门课程统计字段
    private Integer learnerCount; // 学习人数

    private Integer subscriptionCount; // 收藏人数

    private Boolean subscribed; // 是否已收藏

    private Integer progress; // 课程进度百分比 (0-100)
}