package com.prosper.learn.business.util.field;

/**
 * 节点字段常量定义
 * 基于现有的 NodeDTO 版本定义完整的字段集合
 */
public class NodeFields {
    
    /**
     * V1 字段 - NodeDTOV1 的所有字段: id, name
     */
    public static final String[] V1 = {"id", "name"};
    
    /**
     * V2 字段 - NodeDTOV2 的所有字段: id, name, isCompleted
     */
    public static final String[] V2 = {"id", "name", "isCompleted"};
    
    /**
     * 完整字段 - NodeDTO 的所有字段
     */
    public static final String[] FULL = {"id", "name", "description", "courseId", "course", "root", "children", 
                                         "creator", "commentCount", "createdAt", "updatedAt"};
    
    // 私有构造函数，防止实例化
    private NodeFields() {}
}