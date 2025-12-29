package com.prosper.learn.web.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.web.util.MessageUtils;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
     * 处理参数相关异常 - 返回200 + 参数错误码
     * 包括：验证失败、参数缺失、类型不匹配、JSON解析错误、方法不支持、Content-Type不支持
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            IllegalArgumentException.class
    })
    public ApiResponse<Object> handleParameterException(Exception e, HttpServletRequest request) {
        String message = "参数错误";

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
        } else if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) e;
            message = String.format("缺少必需参数: %s", ex.getParameterName());
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            message = String.format("参数类型错误: %s 应为 %s 类型",
                    ex.getName(),
                    ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        } else if (e instanceof HttpMessageNotReadableException) {
            message = "请求体格式错误";
            if (e.getMessage() != null && e.getMessage().contains("JSON parse error")) {
                message = "JSON格式错误或字段类型不匹配";
            }
        } else if (e instanceof HttpRequestMethodNotSupportedException) {
            HttpRequestMethodNotSupportedException ex = (HttpRequestMethodNotSupportedException) e;
            message = String.format("不支持的请求方法: %s", ex.getMethod());
        } else if (e instanceof HttpMediaTypeNotSupportedException) {
            message = "不支持的Content-Type，请使用application/json";
        } else if (e instanceof IllegalArgumentException) {
            // @JsonParam 参数解析异常或其他参数异常
            message = "参数格式错误";
        }

        log.warn("参数异常: {} - {}", e.getClass().getSimpleName(), message);
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