package com.twicemax.user.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * OTP 会话服务 —— 按 scene 管理"已通过某步骤校验的短时凭证"。
 * <p>
 * 仅承载"身份通行证"职责：通过 token 映射到 email，并记录该 session 是否已完成 code 校验。
 * 验证码本身由 {@link OtpCodeService} 独立管理。
 * <p>
 * Redis 结构：
 * <pre>
 *   Key:   otp:session:{scene}:{token}
 *   Type:  Hash
 *   Field: email / verified
 *   TTL:   30 分钟
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpSessionService {

    private static final String KEY_PREFIX = "otp:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final String F_EMAIL = "email";
    private static final String F_VERIFIED = "verified";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建 session。verified 初始为 false。
     */
    public Created create(OtpScene scene, String email) {
        String token = generateToken();
        String k = key(scene, token);
        Map<String, String> fields = new HashMap<>();
        fields.put(F_EMAIL, email);
        fields.put(F_VERIFIED, "false");
        stringRedisTemplate.opsForHash().putAll(k, fields);
        stringRedisTemplate.expire(k, SESSION_TTL);
        return new Created(token, SESSION_TTL.toSeconds());
    }

    /**
     * 根据 token 查找关联邮箱。不存在返回 empty。
     */
    public Optional<String> findEmailByToken(OtpScene scene, String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        Object email = stringRedisTemplate.opsForHash().get(key(scene, token), F_EMAIL);
        return email == null ? Optional.empty() : Optional.of(email.toString());
    }

    /**
     * 凭 token 取出 email；token 不存在抛对应 scene 的 session invalid 错误码。
     */
    public String requireEmail(OtpScene scene, String token) {
        return findEmailByToken(scene, token)
                .orElseThrow(scene.sessionInvalidCode()::exception);
    }

    /**
     * 标记 session 已通过 code 校验。
     */
    public void markVerified(OtpScene scene, String token) {
        stringRedisTemplate.opsForHash().put(key(scene, token), F_VERIFIED, "true");
    }

    /**
     * 要求 session 存在且已完成 code 校验，返回 email。
     */
    public String requireVerifiedEmail(OtpScene scene, String token,
                                       com.twicemax.shared.domain.exception.StatusCode notVerifiedCode) {
        String k = key(scene, token);
        List<Object> values = stringRedisTemplate.opsForHash()
                .multiGet(k, List.of(F_EMAIL, F_VERIFIED));
        Object emailObj = values.get(0);
        if (emailObj == null) {
            throw scene.sessionInvalidCode().exception();
        }
        if (!"true".equals(String.valueOf(values.get(1)))) {
            throw notVerifiedCode.exception();
        }
        return emailObj.toString();
    }

    /**
     * 使 token 立即失效。
     */
    public void invalidate(OtpScene scene, String token) {
        if (token == null || token.isEmpty()) return;
        stringRedisTemplate.delete(key(scene, token));
    }

    /**
     * 查询 token 剩余 TTL（秒）；不存在 / 已过期返回 0。
     */
    public long ttlSeconds(OtpScene scene, String token) {
        if (token == null || token.isEmpty()) return 0;
        Long ttl = stringRedisTemplate.getExpire(key(scene, token), TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) return 0;
        return ttl;
    }

    // ========== 工具 ==========

    private static String key(OtpScene scene, String token) {
        return KEY_PREFIX + scene.key() + ":" + token;
    }

    private static String generateToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record Created(String token, long expiresInSeconds) {}
}
