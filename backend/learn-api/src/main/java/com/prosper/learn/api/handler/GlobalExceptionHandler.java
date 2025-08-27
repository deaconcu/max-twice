package com.prosper.learn.api.handler;

import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.dto.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
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
     * 处理NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public Response<Void> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常", e);
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
    
    /**
     * 处理RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Response<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return Response.fail("系统繁忙，请稍后重试");
    }
}