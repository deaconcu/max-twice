package com.twicemax.web.v2.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.twicemax.analytics.monitoring.service.ErrorLogService;
import com.twicemax.application.dto.v2.ErrorResponse;
import com.twicemax.shared.domain.exception.BusinessException;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.web.util.IpUtils;
import com.twicemax.web.util.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * v2 API 全局异常处理器。
 *
 * <p>核心理念（与 v1 相反）：
 * <ul>
 *   <li>使用语义化 HTTP 状态码（4xx/5xx）替代统一 200</li>
 *   <li>错误体使用 {@link ErrorResponse} 形如 {@code { error: { code, message, details } }}</li>
 *   <li>{@code error.code} 是字符串错误码（{@code StatusCode.name()}），便于前端 if-else</li>
 * </ul>
 *
 * <p>作用域：通过 {@code basePackages} 限定为 {@code com.twicemax.web.v2}，
 * 与 v1 的 {@link com.twicemax.web.handler.GlobalExceptionHandler} 完全隔离。
 * {@code @Order(HIGHEST_PRECEDENCE)} 确保 v2 controller 抛出的异常优先匹配本处理器。
 *
 * <p>覆盖范围：v2 Controller 抛出 + Service/Repository 通过 v2 Controller 调用栈冒泡上来的所有异常。
 */
@RestControllerAdvice(basePackages = "com.twicemax.web.v2")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class V2ApiExceptionHandler {

    private final MessageUtils messageUtils;
    private final ErrorLogService errorLogService;

    // ========== 业务异常 ==========

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("业务异常: [{}] {}", e.getCode(), e.getMessage());

        // i18n 解析（key 用字符串 code）
        String localizedMessage = messageUtils.getMessage("error." + e.getCode(), e.getMessage());
        HttpStatus http = StatusCodeHttpMapper.toHttp(e.getStatusCode());

        return ResponseEntity.status(http).body(ErrorResponse.of(e.getCode(), localizedMessage));
    }

    // ========== Sa-Token 认证/授权 ==========

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ErrorResponse> handleNotLogin(NotLoginException e) {
        log.warn("未登录: {}", e.getMessage());
        StatusCode code = StatusCode.USER_NOT_LOGIN;
        String msg = messageUtils.getMessage("error." + code.name(), code.getMessage());
        return ResponseEntity.status(StatusCodeHttpMapper.toHttp(code))
                .body(ErrorResponse.of(code.name(), msg));
    }

    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<ErrorResponse> handleNotPermission(NotPermissionException e) {
        log.warn("权限不足: {}", e.getMessage());
        StatusCode code = StatusCode.PERMISSION_DENIED;
        String msg = messageUtils.getMessage("error." + code.name(), code.getMessage());
        return ResponseEntity.status(StatusCodeHttpMapper.toHttp(code))
                .body(ErrorResponse.of(code.name(), msg));
    }

    // ========== 参数 / 反序列化 异常 → 400 INVALID_PARAMETER ==========

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
    public ResponseEntity<ErrorResponse> handleParameterException(Exception e) {
        String message = extractParameterMessage(e);
        log.warn("参数异常: {} - {}", e.getClass().getSimpleName(), message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(StatusCode.INVALID_PARAMETER.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("参数验证失败");
        log.warn("参数验证异常: {}", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(StatusCode.INVALID_PARAMETER.name(), message));
    }

    // ========== 路径不存在 → 404 ==========

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        StatusCode code = StatusCode.NOT_FOUND;
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(code.name(), code.getMessage()));
    }

    // ========== 兜底 → 500 ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("系统异常", e);
        recordError(e, request);

        StatusCode code = StatusCode.SYSTEM_ERROR;
        String msg = messageUtils.getMessage("error." + code.name(), code.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(code.name(), msg));
    }

    // ========== 私有工具 ==========

    private String extractParameterMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex) {
            if (ex.getBindingResult().hasFieldErrors()) {
                return ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            }
            return "参数验证失败";
        }
        if (e instanceof BindException ex) {
            if (ex.getBindingResult().hasFieldErrors()) {
                return ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            }
            return "参数绑定失败";
        }
        if (e instanceof MissingServletRequestParameterException ex) {
            return String.format("缺少必需参数: %s", ex.getParameterName());
        }
        if (e instanceof MethodArgumentTypeMismatchException ex) {
            return String.format("参数类型错误: %s 应为 %s 类型",
                    ex.getName(),
                    ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知");
        }
        if (e instanceof HttpMessageNotReadableException) {
            if (e.getMessage() != null && e.getMessage().contains("JSON parse error")) {
                return "JSON 格式错误或字段类型不匹配";
            }
            return "请求体格式错误";
        }
        if (e instanceof HttpRequestMethodNotSupportedException ex) {
            return String.format("不支持的请求方法: %s", ex.getMethod());
        }
        if (e instanceof HttpMediaTypeNotSupportedException) {
            return "不支持的 Content-Type，请使用 application/json";
        }
        if (e instanceof java.time.format.DateTimeParseException) {
            return "无效的日期格式";
        }
        if (e instanceof IllegalArgumentException) {
            return "参数格式错误";
        }
        return "参数错误";
    }

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

    private Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
