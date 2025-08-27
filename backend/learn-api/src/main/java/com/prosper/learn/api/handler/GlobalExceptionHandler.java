package com.prosper.learn.api.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Response<Object> handleNotLoginException(NotLoginException e) {
        return Response.notLogin;
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Response<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());
        return Response.fail(e.getMessage());
    }
    
    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Response.fail("参数不正确: " + e.getMessage());
    }
    
    /**
     * 处理JSON处理异常
     */
    @ExceptionHandler(JsonProcessingException.class)
    public Response<Void> handleJsonProcessingException(JsonProcessingException e) {
        log.error("JSON处理异常: {}", e.getMessage(), e);
        return Response.fail("数据格式错误");
    }
    
    /**
     * 处理数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Response<Void> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问异常: {}", e.getMessage(), e);
        return Response.fail("数据访问失败，请稍后重试");
    }
    
    /**
     * 处理NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public Response<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
        return Response.fail("系统繁忙，请稍后重试");
    }
    
    /**
     * 处理RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Response<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return Response.fail("系统繁忙，请稍后重试");
    }
    
    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Response.fail("系统繁忙，请稍后重试");
    }
}