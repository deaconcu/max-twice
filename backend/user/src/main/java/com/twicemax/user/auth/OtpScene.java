package com.twicemax.user.auth;

import com.twicemax.shared.domain.exception.StatusCode;

/**
 * OTP 使用场景枚举。
 * <p>
 * 用于区分不同业务流程的 OTP code 和 session 的 Redis key 前缀，
 * 以及 session 失效时应抛出的错误码。
 */
public enum OtpScene {

    /**
     * 注册 / 登录时的邮箱验证。
     */
    REGISTER("register", StatusCode.PENDING_SESSION_INVALID),

    /**
     * 忘记密码重置流程。
     */
    PASSWORD_RESET("password_reset", StatusCode.PASSWORD_RESET_SESSION_INVALID);

    private final String key;
    private final StatusCode sessionInvalidCode;

    OtpScene(String key, StatusCode sessionInvalidCode) {
        this.key = key;
        this.sessionInvalidCode = sessionInvalidCode;
    }

    public String key() {
        return key;
    }

    public StatusCode sessionInvalidCode() {
        return sessionInvalidCode;
    }
}
