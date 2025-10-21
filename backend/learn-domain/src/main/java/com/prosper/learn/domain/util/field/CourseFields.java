package com.prosper.learn.domain.util.field;

/**
 * 课程字段常量定义
 * 基于现有的 CourseDTO 版本定义完整的字段集合
 */
public class CourseFields {
    
    /**
     * V2 字段 - CourseDTOV2 的所有字段: id, name, description, mainCategory, subCategory
     */
    public static final String[] V2 = {"id", "name", "description", "mainCategory", "subCategory"};
    
    /**
     * V3 字段 - CourseDTOV3 的所有字段: id, name
     */
    public static final String[] V3 = {"id", "name"};
    
    /**
     * V4 字段 - CourseDTOV4 的所有字段
     */
    public static final String[] V4 = {"id", "name", "description", "creator", "rootNode", "parent", 
                                        "state", "mainCategory", "subCategory", "reason",
                                        "createdAt", "updatedAt", "learnerCount", "subscriptionCount", 
                                        "subscribed", "progress"};
    
    /**
     * 完整字段 - CourseDTO 的所有字段
     */
    public static final String[] FULL = {"id", "name", "description", "creator", "rootNode", "parentId", 
                                         "state", "mainCategory", "subCategory", "reason",
                                         "createdAt", "updatedAt"};
    
    // 私有构造函数，防止实例化
    private CourseFields() {}
}