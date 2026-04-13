package com.prosper.learn.web.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.analytics.monitoring.service.ErrorLogService;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.web.util.IpUtils;
import com.prosper.learn.web.util.MessageUtils;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 全局异常处理器
 *
 * 核心理念：统一返回 HTTP 200，通过 ApiResponse.code 字段表示业务状态
 * - 所有异常：返回 200 + 业务错误码
 * - 前端通过 response.code 判断业务成功或失败
 * - 简化前端错误处理，避免浏览器/网关对 4xx/5xx 的特殊处理
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageUtils messageUtils;
    private final ErrorLogService errorLogService;

    /**
     * 处理未登录异常 - 返回200 + 业务错误码
     */
    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Object> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        return ApiResponse.unauthorized("用户未登录").path(request.getRequestURI());
    }

    /**
     * 处理权限不足异常 - 返回200 + 业务错误码
     */
    @ExceptionHandler(NotPermissionException.class)
    public ApiResponse<Object> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("权限不足: {}", e.getMessage());
        return ApiResponse.forbidden();
    }

    /**
     * 处理业务异常 - 返回200 + 业务错误码
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());

        // 根据错误码获取国际化消息，如果没有找到则使用原消息
        String errorKey = "error." + e.getCode();
        String localizedMessage = messageUtils.getMessage(errorKey, e.getMessage());

        return ApiResponse.fail(e.getCode(), localizedMessage).path(request.getRequestURI());
    }
    
    /**
     * 处理参数相关异常 - 返回200 + 参数错误码
     * 包括：验证失败、参数缺失、类型不匹配、JSON解析错误、方法不支持、Content-Type不支持、日期格式错误
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            IllegalArgumentException.class,
            java.time.format.DateTimeParseException.class
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
        } else if (e instanceof java.time.format.DateTimeParseException) {
            message = "无效的日期格式，请使用 yyyy-MM-dd";
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
     * 处理资源未找到异常（路径参数缺失或路径不存在）- 返回200 + NOT_FOUND错误码
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("资源未找到: {}", e.getMessage());
        return ApiResponse.fail(StatusCode.NOT_FOUND.getCode(), StatusCode.NOT_FOUND.getMessage())
                .path(request.getRequestURI());
    }

    /**
     * 处理所有其他异常 - 返回 200 + 系统繁忙
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);

        // 记录错误日志
        recordError(e, request);

        return ApiResponse.fail("系统繁忙，请稍后重试").path(request.getRequestURI());
    }

    /**
     * 记录错误到数据库
     */
    private void recordError(Exception e, HttpServletRequest request) {
        try {
            String errorType = e.getClass().getSimpleName();
            String message = e.getMessage();
            String stackTrace = getStackTrace(e);
            String url = request.getRequestURI();
            Long userId = getCurrentUserId();
            String ip = IpUtils.getIpAddress(request);

            errorLogService.recordBackendError(errorType, message, stackTrace, url, userId, ip);
        } catch (Exception ex) {
            log.error("记录错误日志失败", ex);
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}