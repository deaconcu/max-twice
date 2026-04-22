package com.twicemax.user.auth;

import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 密码重置服务 —— 以 resetSessionToken 为主键，管理重置流程全部状态。
 * <p>
 * 与注册/登录的 {@link EmailVerifySessionService} + {@link EmailVerificationCodeService}
 * 分开，避免两种流程的验证码互相覆盖。
 * <p>
 * Redis 结构：
 * <pre>
 *   Key:   password_reset:{token}
 *   Type:  Hash
 *   Field: email / code / codeExpiresAt / attemptsLeft / resendCount / lastResendAt / verified
 *   TTL:   30 分钟
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String KEY_PREFIX = "password_reset:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final String F_EMAIL = "email";
    private static final String F_CODE = "code";
    private static final String F_CODE_EXPIRES_AT = "codeExpiresAt";
    private static final String F_ATTEMPTS_LEFT = "attemptsLeft";
    private static final String F_RESEND_COUNT = "resendCount";
    private static final String F_LAST_RESEND_AT = "lastResendAt";
    private static final String F_VERIFIED = "verified";

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RESEND_COUNT = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final SystemProperties systemProperties;

    // ========== 对外 API ==========

    /**
     * 创建 reset session + 签发验证码。返回 token/code/resendAvailableIn。
     */
    public Created createSessionAndIssueCode(String email) {
        String token = generateToken();
        String code = generateCode();
        long now = Instant.now().getEpochSecond();
        long codeExpiresAt = now + codeExpirySeconds();

        Map<String, String> fields = new HashMap<>();
        fields.put(F_EMAIL, email);
        fields.put(F_CODE, code);
        fields.put(F_CODE_EXPIRES_AT, String.valueOf(codeExpiresAt));
        fields.put(F_ATTEMPTS_LEFT, String.valueOf(MAX_ATTEMPTS));
        fields.put(F_RESEND_COUNT, "0");
        fields.put(F_LAST_RESEND_AT, String.valueOf(now));
        fields.put(F_VERIFIED, "false");

        String k = key(token);
        stringRedisTemplate.opsForHash().putAll(k, fields);
        stringRedisTemplate.expire(k, SESSION_TTL);
        log.info("创建密码重置会话: email={}", email);
        return new Created(token, code, SESSION_TTL.toSeconds(), sendIntervalSeconds());
    }

    /**
     * 重发验证码（使用现有 token），返回新 code 和下次可重发剩余秒数。
     */
    public Resent resend(String token) {
        String k = key(token);
        Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(k);
        if (raw.isEmpty()) {
            throw StatusCode.PASSWORD_RESET_SESSION_INVALID.exception();
        }
        long now = Instant.now().getEpochSecond();
        long lastResendAt = parseLong(raw.get(F_LAST_RESEND_AT), 0);
        if (now - lastResendAt < sendIntervalSeconds()) {
            throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
        }
        int resendCount = parseInt(raw.get(F_RESEND_COUNT), 0);
        if (resendCount >= MAX_RESEND_COUNT) {
            throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
        }

        String code = generateCode();
        long codeExpiresAt = now + codeExpirySeconds();

        Map<String, String> fields = new HashMap<>();
        fields.put(F_CODE, code);
        fields.put(F_CODE_EXPIRES_AT, String.valueOf(codeExpiresAt));
        fields.put(F_ATTEMPTS_LEFT, String.valueOf(MAX_ATTEMPTS));
        fields.put(F_RESEND_COUNT, String.valueOf(resendCount + 1));
        fields.put(F_LAST_RESEND_AT, String.valueOf(now));
        // 重发后需重新验证
        fields.put(F_VERIFIED, "false");

        stringRedisTemplate.opsForHash().putAll(k, fields);
        // TTL 不重置，继续用 session 的剩余生命周期
        String email = String.valueOf(raw.get(F_EMAIL));
        log.info("重发重置验证码: email={}, resendCount={}", email, resendCount + 1);
        long remainingTtl = Math.max(0, ttlSeconds(token));
        return new Resent(email, code, sendIntervalSeconds(), remainingTtl);
    }

    /**
     * 校验验证码，成功则置 verified=true（不删 key，后续 confirm 需用）。
     */
    public void verifyCode(String token, String inputCode) {
        String k = key(token);
        List<Object> values = stringRedisTemplate.opsForHash()
                .multiGet(k, List.of(F_CODE, F_CODE_EXPIRES_AT, F_ATTEMPTS_LEFT, F_EMAIL));
        Object codeObj = values.get(0);
        if (codeObj == null) {
            throw StatusCode.PASSWORD_RESET_SESSION_INVALID.exception();
        }
        long expiresAt = parseLong(values.get(1), 0);
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw StatusCode.USER_VERIFICATION_CODE_EXPIRED.exception();
        }

        if (codeObj.toString().equals(inputCode)) {
            stringRedisTemplate.opsForHash().put(k, F_VERIFIED, "true");
            return;
        }

        int attemptsLeft = parseInt(values.get(2), 0);
        if (attemptsLeft <= 1) {
            // 清掉 code 字段，强制重发（保留 session 以便用户重试流程）
            stringRedisTemplate.opsForHash().delete(k, F_CODE, F_CODE_EXPIRES_AT, F_ATTEMPTS_LEFT);
            throw StatusCode.USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED.exception();
        }
        stringRedisTemplate.opsForHash().increment(k, F_ATTEMPTS_LEFT, -1);
        throw StatusCode.USER_VERIFICATION_CODE_INVALID.exception();
    }

    /**
     * 要求 token 存在且已完成验证码验证，返回 email。
     */
    public String requireVerifiedEmail(String token) {
        String k = key(token);
        List<Object> values = stringRedisTemplate.opsForHash()
                .multiGet(k, List.of(F_EMAIL, F_VERIFIED));
        Object emailObj = values.get(0);
        if (emailObj == null) {
            throw StatusCode.PASSWORD_RESET_SESSION_INVALID.exception();
        }
        if (!"true".equals(String.valueOf(values.get(1)))) {
            throw StatusCode.PASSWORD_RESET_NOT_VERIFIED.exception();
        }
        return emailObj.toString();
    }

    /**
     * 立即失效 token。
     */
    public void invalidate(String token) {
        if (token == null || token.isEmpty()) return;
        stringRedisTemplate.delete(key(token));
    }

    /**
     * 查询 token 剩余 TTL（秒）；不存在 / 已过期返回 0。
     */
    public long ttlSeconds(String token) {
        if (token == null || token.isEmpty()) return 0;
        Long ttl = stringRedisTemplate.getExpire(key(token), TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) return 0;
        return ttl;
    }

    // ========== 工具 ==========

    private static String key(String token) {
        return KEY_PREFIX + token;
    }

    private int codeExpirySeconds() {
        return systemProperties.getUser().getVerificationCodeExpiryMinutes() * 60;
    }

    private int sendIntervalSeconds() {
        return systemProperties.getUser().getVerificationCodeSendIntervalSeconds();
    }

    private static String generateToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateCode() {
        int n = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(n);
    }

    private static long parseLong(Object v, long dft) {
        if (v == null) return dft;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return dft; }
    }

    private static int parseInt(Object v, int dft) {
        if (v == null) return dft;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return dft; }
    }

    /**
     * 创建结果：token、code、session TTL、下次可重发剩余秒数。
     */
    public record Created(String token, String code, long expiresInSeconds, long resendAvailableInSeconds) {}

    /**
     * 重发结果：email、新 code、下次可重发剩余秒数、session 剩余 TTL 秒数。
     */
    public record Resent(String email, String code, long resendAvailableInSeconds, long expiresInSeconds) {}
}
