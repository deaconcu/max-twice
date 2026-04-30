package com.twicemax.shared.domain.exception;

import java.util.Map;

/**
 * 业务异常 - 用户可理解的错误。
 * <p>
 * 同时持有字符串错误码（v2 用，如 "INVITE_ONLY"）和数字错误码（v1 用，如 1127），
 * 让 v1 / v2 异常处理器各取所需，互不影响。
 * <p>
 * 可携带 {@code details} 结构化数据（v2 协议透传到 {@code error.details}），
 * 用于把字段级错误、失效引用列表、retryAfter 等附加信息传给前端。
 */
public class BusinessException extends RuntimeException {

    private final String code;       // 字符串错误码（StatusCode.name()），v2 用
    private final int legacyCode;    // 数字错误码（StatusCode.getCode()），v1 用
    private final String message;
    private final StatusCode statusCode;  // 原始枚举引用，可空（兼容直接构造的旧调用）
    private final Map<String, Object> details; // v2 协议 error.details，可空

    /**
     * 推荐的主构造器：从 StatusCode 构造，两个 code 都有。
     */
    public BusinessException(StatusCode statusCode, String localizedMessage) {
        this(statusCode, localizedMessage, (Map<String, Object>) null);
    }

    public BusinessException(StatusCode statusCode, String localizedMessage, Throwable cause) {
        super(localizedMessage, cause);
        this.statusCode = statusCode;
        this.code = statusCode.name();
        this.legacyCode = statusCode.getCode();
        this.message = localizedMessage;
        this.details = null;
    }

    /**
     * 携带结构化 details 的构造器（v2 透传到 error.details）。
     */
    public BusinessException(StatusCode statusCode, String localizedMessage, Map<String, Object> details) {
        super(localizedMessage);
        this.statusCode = statusCode;
        this.code = statusCode.name();
        this.legacyCode = statusCode.getCode();
        this.message = localizedMessage;
        this.details = details;
    }

    /**
     * 兼容老调用：只传字符串 code（无枚举引用，legacyCode 置 0）。
     * 不推荐用于新代码。
     */
    public BusinessException(String code, String message) {
        super(message);
        this.statusCode = null;
        this.code = code;
        this.legacyCode = 0;
        this.message = message;
        this.details = null;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.code = code;
        this.legacyCode = 0;
        this.message = message;
        this.details = null;
    }

    public String getCode() {
        return code;
    }

    public int getLegacyCode() {
        return legacyCode;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
