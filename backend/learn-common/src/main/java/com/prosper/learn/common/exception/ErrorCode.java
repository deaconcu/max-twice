package com.prosper.learn.common.exception;

/**
 * 错误码枚举
 */
public enum ErrorCode {
    
    // 用户认证相关 A0xxx
    USER_NOT_LOGIN("A0001", "用户未登录"),
    PERMISSION_DENIED("A0002", "权限不足"),
    USER_NOT_FOUND("A0003", "user.not.found"),
    USER_ALREADY_EXISTS("A0004", "user.already.exists"),
    USER_PASSWORD_WRONG("A0005", "user.password.wrong"),
    USER_EMAIL_NOT_VALIDATED("A0006", "user.email.not.validated"),

    // 通用业务相关 B00xx
    NOT_SUPPORTED("B0001", "不支持的操作类型"),
    INVALID_PARAMETER("B0002", "参数异常"),
    JSON_PROCESSING_ERROR("B0003", "JSON处理异常"),

    // 课程相关 B01xx
    COURSE_NOT_FOUND("B0101", "课程不存在"),

    // 路线图相关 B02xx
    ROADMAP_NOT_FOUND("B0201", "路线图不存在"),
    ROADMAP_CONTENT_INVALID("B0202", "路线图内容格式不正确"),
    ROADMAP_PIN_LIMIT_EXCEEDED("B0203", "最多只能置顶19个路线图"),
    
    // 学习进度相关 B03xx
    USER_ROADMAP_NOT_FOUND("B0301", "学习记录不存在"),
    USER_COURSE_NOT_FOUND("B0302", "课程学习记录不存在"),
    
    // 数据解析相关 B04xx
    JSON_PARSE_ERROR("B0401", "数据格式解析失败"),
    CONTENT_HASH_ERROR("B0402", "内容哈希计算失败"),

    // 评论相关 B05xx
    COMMENT_NOT_FOUND("B0501", "评论不存在"),

    // 内容管理相关 B06xx
    CONTENTS_COURSE_NOT_FOUND("B0601", "课程不存在"),
    CONTENTS_POST_NOT_FOUND("B0602", "帖子不存在"), 
    CONTENTS_PINNED_ITEMS_LIMIT_EXCEEDED("B0603", "置顶帖子数量超限"),
    CONTENTS_INVALID_POST_TYPE("B0604", "无效的帖子类型"),

    // 目录管理相关 B07xx
    TOC_USER_TOC_NOT_FOUND("B0701", "用户目录不存在"),
    TOC_INDEX_OUT_OF_BOUNDS("B0702", "目录索引超出范围"),

    // 系统错误 S0xxx
    SYSTEM_ERROR("S0001", "系统繁忙，请稍后重试"),
    DATABASE_ERROR("S0002", "数据访问异常"),
    EXTERNAL_SERVICE_ERROR("S0003", "外部服务调用失败");

    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 创建业务异常
     */
    public BusinessException exception() {
        return new BusinessException(this.code, this.message);
    }
    
    /**
     * 创建支持国际化的业务异常
     */
    public BusinessException exception(String localizedMessage) {
        return new BusinessException(this.code, localizedMessage);
    }
    
    /**
     * 创建带原因的业务异常
     */
    public BusinessException exception(Throwable cause) {
        return new BusinessException(this.code, this.message, cause);
    }
}