package com.prosper.learn.business.util.field;

/**
 * 评论字段常量定义
 * 基于现有的 CommentDTO 版本定义完整的字段集合
 */
public class CommentFields {
    
    /**
     * V1 字段 - CommentDTOV1 的所有字段（包含 state 字段）
     */
    public static final String[] V1 = {"id", "content", "type", "objectId", "replyCount", "replyTo", 
                                       "fromUser", "toUser", "upvoteCount", "state", "createdAt"};
    
    /**
     * 完整字段 - CommentDTO 的所有字段（包含 upvoted 和 children 字段）
     */
    public static final String[] FULL = {"id", "content", "type", "objectId", "replyCount", "replyTo", 
                                         "fromUser", "toUser", "upvoteCount", "upvoted", "createdAt", "children"};
    
    // 私有构造函数，防止实例化
    private CommentFields() {}
}