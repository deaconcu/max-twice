package com.twicemax.user.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱验证会话（pending session）
 * <p>
 * 由 Redis 存储，token 作为 key 的后缀，本对象对应 key 下 Hash 的所有 field。
 * <p>
 * 存活期内用户可多次提交验证码（受 attemptsLeft 限制），验证成功后整条记录删除。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerifySession {

    /** 关联邮箱 */
    private String email;

    /** 当前 6 位验证码 */
    private String code;

    /** code 的过期时间戳（epoch second）。注意：session 本身另有 Redis TTL */
    private long codeExpiresAt;

    /** 剩余验证尝试次数 */
    private int attemptsLeft;

    /** 上次重发验证码时间戳（epoch second），用于限频 */
    private long lastResendAt;

    /** 累计重发次数 */
    private int resendCount;
}
