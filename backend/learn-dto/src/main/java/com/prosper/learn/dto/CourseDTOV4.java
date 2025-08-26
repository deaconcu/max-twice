package com.prosper.learn.dto;

import lombok.Data;

@Data
public class CourseDTOV4 {

    private Integer id;

    private String name;

    private String description;

    private Integer creator;

    private Integer rootNode;

    //private Integer parent;
    private CourseDTOV3 parent;

    private String state; // 修改为 String 类型，支持 SUMMITTED, APPROVED, REJECTED

    private Integer mainCategory; // 新增主分类字段

    private Integer subCategory; // 新增子分类字段

    private String rejectedReason; // 拒绝原因，默认为空字符串

    private String cTime;

    private String uTime;

    // 新增：热门课程统计字段
    private Integer learnerCount; // 学习人数

    private Integer subscriptionCount; // 收藏人数

    private boolean subscribed; // 是否已收藏

    private Integer progress; // 课程进度百分比 (0-100)
}