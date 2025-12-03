package com.prosper.learn.business.util.field;

/**
 * 帖子字段常量定义
 * 基于现有的 PostDTO 版本定义完整的字段集合
 */
public class PostFields {
    
    /**
     * V2 字段 - PostDTOV2 的所有字段（包含 views 字段）
     */
    public static final String[] V2 = {"id", "content", "nodeId", "node", "creatorId", "creator", "type", 
                                       "once", "twice", "helpful", "commentCount", "views", "state", 
                                       "score", "createdAt", "updatedAt", "voteType"};
    
    /**
     * 完整字段 - PostDTO 的所有字段（不包含 views 字段）
     */
    public static final String[] FULL = {"id", "content", "nodeId", "node", "creatorId", "creator", "type", 
                                         "once", "twice", "helpful", "commentCount", "state", "score", 
                                         "createdAt", "updatedAt", "voteType"};
    
    // 私有构造函数，防止实例化
    private PostFields() {}
}