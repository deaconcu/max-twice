package com.prosper.learn.domain.exception;

/**
 * 错误码枚举
 */
public enum ErrorCode {
    
    // 用户认证相关 A0xxx
    USER_NOT_LOGIN("A0001", "用户未登录"),
    PERMISSION_DENIED("A0002", "权限不足"),
    
    // 路线图相关 B0xxx
    ROADMAP_NOT_FOUND("B0001", "路线图不存在"),
    ROADMAP_CONTENT_INVALID("B0002", "路线图内容格式不正确"),
    ROADMAP_PIN_LIMIT_EXCEEDED("B0003", "最多只能置顶19个路线图"),
    
    // 学习进度相关 C0xxx
    USER_ROADMAP_NOT_FOUND("C0001", "学习记录不存在"),
    USER_COURSE_NOT_FOUND("C0002", "课程学习记录不存在"),
    
    // 数据解析相关 D0xxx
    JSON_PARSE_ERROR("D0001", "数据格式解析失败"),
    CONTENT_HASH_ERROR("D0002", "内容哈希计算失败"),
    
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
     * 创建带原因的业务异常
     */
    public BusinessException exception(Throwable cause) {
        return new BusinessException(this.code, this.message, cause);
    }
}