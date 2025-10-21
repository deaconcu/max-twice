package com.prosper.learn.dto.response;

import lombok.Data;

/**
 * 统一的课程DTO
 * 包含CourseDTOV1-V4的所有字段，不舍弃任何字段
 */
@Data
public class CourseDTO {

    // === 基础字段 (所有版本都有) ===
    private Long id;
    private String name;
    
    // === 描述字段 (V1, V2, V4有) ===
    private String description;
    
    // === 关联字段 (V1, V4有) ===
    private Long creatorId;
    private Long creator;
    private Long rootNodeId;
    private Long parentCourseId;
    
    // === 父课程信息 (V4特有的嵌套对象) ===
    private CourseDTO parentCourse;  // 保持原有的CourseDTOV3类型
    
    // === 状态字段 (V1, V4有) ===
    private Byte state; // SUBMITTED, APPROVED, REJECTED
    
    // === 分类字段 (V1, V2, V4有) ===
    private Integer mainCategory;
    private Integer subCategory;
    
    // === 管理字段 (V1, V4有) ===
    private String reason;
    
    // === 时间字段 (V1, V4有) ===
    private String createdAt;
    private String updatedAt;
    
    // === 统计字段 (V4特有) ===
    private Integer learnerCount;     // 学习人数
    private Integer subscriptionCount; // 收藏人数
    private Boolean subscribed;       // 是否已收藏
    private Integer progress;         // 课程进度百分比 (0-100)
}