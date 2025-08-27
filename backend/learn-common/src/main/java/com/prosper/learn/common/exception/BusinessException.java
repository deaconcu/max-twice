package com.prosper.learn.common.exception;

/**
 * 业务异常 - 用户可理解的错误
 */
public class BusinessException extends RuntimeException {
    
    private final String code;
    private final String message;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public String getMessage() {
        return message;
    }
}