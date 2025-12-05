package com.prosper.learn.web.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.prosper.learn.web.util.MessageUtils;
import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 
 * 核心理念：HTTP状态码表示传输层状态，业务状态码表示业务层结果
 * - 业务异常：返回200 + 业务错误码
 * - 其他所有异常：返回500 + 系统繁忙
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtils messageUtils;

    /**
     * 处理未登录异常 - 返回200 + 业务错误码
     */
    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Object> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        return ApiResponse.unauthorized("用户未登录").path(request.getRequestURI());
    }

    /**
     * 处理业务异常 - 返回200 + 业务错误码
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());
        
        // 尝试获取国际化消息，如果失败则使用原消息
        String localizedMessage;
        try {
            localizedMessage = messageUtils.getMessage(e.getMessage(), e.getMessage());
        } catch (Exception ex) {
            localizedMessage = e.getMessage();
        }
        
        return ApiResponse.error(e.getCode(), localizedMessage).path(request.getRequestURI());
    }
    
    /**
     * 处理参数验证异常 - 返回200 + 参数错误码
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Object> handleValidationException(Exception e, HttpServletRequest request) {
        String message = "参数验证失败";

        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().hasFieldErrors()) {
                message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            if (ex.getBindingResult().hasFieldErrors()) {
                message = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            }
        }

        log.warn("参数验证异常: {}", message);
        return ApiResponse.paramError(message).path(request.getRequestURI());
    }

    /**
     * 处理 @RequestParam/@PathVariable 参数验证异常 - 返回200 + 参数错误码
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Object> handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数验证失败");

        log.warn("参数验证异常: {}", message);
        return ApiResponse.paramError(message).path(request.getRequestURI());
    }
    
    /**
     * 处理所有其他异常 - 返回500 + 系统繁忙
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        ApiResponse<Object> response = ApiResponse.error("系统繁忙，请稍后重试")
                .path(request.getRequestURI());
        return ResponseEntity.status(500).body(response);
    }
}