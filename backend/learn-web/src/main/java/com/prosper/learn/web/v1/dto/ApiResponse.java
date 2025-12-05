package com.prosper.learn.web.v1.dto;

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

    // ========== 请求未到达业务层 ==========

    /**
     * 参数错误响应，没到达业务逻辑层，默认422
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(422, message, null);
    }

    /**
     * 未授权响应, 默认401
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }

    // ========== 请求到达业务层且成功响应 ==========

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
     * 成功响应 (带数据)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    // ========== 请求到达业务层，但是发生系统异常或者未知错误 ==========

    /**
     * 错误响应 (带自定义错误码)
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

    // ========== 其他方法 ==========

    /**
     * 设置请求路径
     */
    public ApiResponse<T> path(String path) {
        this.path = path;
        return this;
    }
}