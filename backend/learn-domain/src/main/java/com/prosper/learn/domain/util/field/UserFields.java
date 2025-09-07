package com.prosper.learn.domain.util.field;

/**
 * 用户字段常量定义
 * 基于现有的 UserDTO 版本定义完整的字段集合
 */
public class UserFields {
    
    /**
     * V1 字段 - UserDTOV1 的所有字段: id, name, biography
     */
    public static final String[] V1 = {"id", "name", "biography"};
    
    /**
     * V2 字段 - UserDTOV2 的所有字段: id, name, subscriptions
     */
    public static final String[] V2 = {"id", "name", "subscriptions"};
    
    /**
     * V3 字段 - UserDTOV3 的所有字段: id, name, biography, followed
     */
    public static final String[] V3 = {"id", "name", "biography", "followed"};
    
    /**
     * V4 字段 - UserDTOV4 的所有字段: id, name
     */
    public static final String[] V4 = {"id", "name"};
    
    /**
     * 完整字段 - UserDTO 的所有字段
     */
    public static final String[] FULL = {"id", "name", "password", "phone", "email", "emailValidated", "biography", "createdAt", "updatedAt"};
    
    // 私有构造函数，防止实例化
    private UserFields() {}
}