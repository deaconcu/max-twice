package com.prosper.learn.domain.util.field;

/**
 * 路线图字段常量定义
 * 基于现有的 RoadmapDTO 版本定义完整的字段集合
 */
public class RoadmapFields {
    
    /**
     * V2 字段 - RoadmapDTOV2 的所有字段
     */
    public static final String[] V2 = {"id", "content", "profession", "description", "vote", "comment", 
                                       "upvoted", "creator", "updatedAt", "createdAt"};
    
    /**
     * 完整字段 - RoadmapDTO 的所有字段
     */
    public static final String[] FULL = {"id", "content", "professionId", "description", "vote", "comment", 
                                         "upvoted", "pinned", "learning", "creator", "updatedAt", "createdAt"};
    
    // 私有构造函数，防止实例化
    private RoadmapFields() {}
}