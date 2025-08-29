package com.prosper.learn.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一API响应格式
 * 
 * @param <T> 数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    private String path;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 错误响应（默认500）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
    
    /**
     * 业务错误响应
     */
    public static <T> ApiResponse<T> businessError(String message) {
        return new ApiResponse<>(400, message, null);
    }
    
    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(422, message, null);
    }
    
    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }
    
    /**
     * 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message, null);
    }
    
    /**
     * 资源不存在响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }
    
    /**
     * 设置请求路径
     */
    public ApiResponse<T> path(String path) {
        this.path = path;
        return this;
    }
}