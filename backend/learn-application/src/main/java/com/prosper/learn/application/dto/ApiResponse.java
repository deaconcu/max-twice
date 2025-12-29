package com.prosper.learn.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.Data;

/**
 * 统一API响应格式
 * HTTP状态码均返回200，具体业务状态通过code字段区分
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
     * 参数错误响应，没到达业务逻辑层，默认400
     */
    public static <T> ApiResponse<T> paramError(String message) {
        return new ApiResponse<>(StatusCode.INVALID_PARAMETER.getCode(), message, null);
    }

    /**
     * 未授权响应，返回用户未登录错误码 1101
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(StatusCode.USER_NOT_LOGIN.getCode(), message, null);
    }

    /**
     * 权限不足响应，返回固定错误码 1006
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(StatusCode.PERMISSION_DENIED.getCode(), "权限不足", null);
    }

    // ========== 请求到达业务层且成功响应 ==========

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(StatusCode.OK.getCode(), "操作成功", null);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(StatusCode.OK.getCode(), message, data);
    }

    /**
     * 成功响应 (带数据，用于命令操作)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(StatusCode.OK.getCode(), "操作成功", data);
    }

    /**
     * 查询响应 (带数据，不返回message)
     * 用于查询类接口，只返回 code 和 data
     */
    public static <T> ApiResponse<T> query(T data) {
        return new ApiResponse<>(StatusCode.OK.getCode(), null, data);
    }

    // ========== 请求到达业务层，但是发生系统异常或者未知错误 ==========

    /**
     * 错误响应 (带自定义错误码)
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 错误响应（默认系统错误）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(StatusCode.UNKNOWN_EXCEPTION.getCode(), message, null);
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