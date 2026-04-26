package com.twicemax.shared.domain.exception;

/**
 * 业务异常 - 用户可理解的错误。
 * <p>
 * 同时持有字符串错误码（v2 用，如 "INVITE_ONLY"）和数字错误码（v1 用，如 1127），
 * 让 v1 / v2 异常处理器各取所需，互不影响。
 */
public class BusinessException extends RuntimeException {

    private final String code;       // 字符串错误码（StatusCode.name()），v2 用
    private final int legacyCode;    // 数字错误码（StatusCode.getCode()），v1 用
    private final String message;
    private final StatusCode statusCode;  // 原始枚举引用，可空（兼容直接构造的旧调用）

    /**
     * 推荐的主构造器：从 StatusCode 构造，两个 code 都有。
     */
    public BusinessException(StatusCode statusCode, String localizedMessage) {
        super(localizedMessage);
        this.statusCode = statusCode;
        this.code = statusCode.name();
        this.legacyCode = statusCode.getCode();
        this.message = localizedMessage;
    }

    public BusinessException(StatusCode statusCode, String localizedMessage, Throwable cause) {
        super(localizedMessage, cause);
        this.statusCode = statusCode;
        this.code = statusCode.name();
        this.legacyCode = statusCode.getCode();
        this.message = localizedMessage;
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
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.code = code;
        this.legacyCode = 0;
        this.message = message;
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

    @Override
    public String getMessage() {
        return message;
    }
}
