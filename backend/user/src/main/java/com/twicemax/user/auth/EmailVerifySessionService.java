package com.twicemax.user.auth;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * 邮箱验证会话服务 —— 证明"此用户刚通过密码校验"的短时凭证。
 * <p>
 * 仅承载"身份通行证"职责，不存储验证码。验证码由 {@link EmailVerificationCodeService} 独立管理。
 * <p>
 * Redis 结构：
 * <pre>
 *   Key:   verify_session:{token}
 *   Type:  String
 *   Value: email（所属邮箱）
 *   TTL:   30 分钟
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerifySessionService {

    private static final String KEY_PREFIX = "verify_session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建新的 session 并返回 token。
     * <p>
     * 每次调用都生成新 token；旧 token（若有）不会被清理，靠 Redis TTL 自然过期。
     */
    public Created create(String email) {
        String token = generateToken();
        stringRedisTemplate.opsForValue().set(key(token), email, SESSION_TTL);
        return new Created(token, SESSION_TTL.toSeconds());
    }

    /**
     * 根据 token 查找关联邮箱。不存在返回 empty。
     */
    public Optional<String> findEmailByToken(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key(token)));
    }

    /**
     * 凭 token 取出 email。token 不存在抛 PENDING_SESSION_INVALID。
     */
    public String requireEmail(String token) {
        return findEmailByToken(token)
                .orElseThrow(StatusCode.PENDING_SESSION_INVALID::exception);
    }

    /**
     * 使 token 立即失效。
     */
    public void invalidate(String token) {
        if (token == null || token.isEmpty()) return;
        stringRedisTemplate.delete(key(token));
    }

    // ========== 工具 ==========

    private static String key(String token) {
        return KEY_PREFIX + token;
    }

    private static String generateToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Created(String token, long expiresInSeconds) {}
}
