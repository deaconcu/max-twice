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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 邮箱验证码服务 —— 管理验证码、失败次数、重发限频。
 * <p>
 * 与 {@link EmailVerifySessionService} 职责分离：此服务不关心 session token，
 * 只以 email 为键管理"验证码及其尝试/重发记录"。
 * <p>
 * Redis 结构：
 * <pre>
 *   Key:   email_verify_code:{email}
 *   Type:  Hash
 *   Field: code / codeExpiresAt / attemptsLeft / resendCount / lastResendAt
 *   TTL:   由 SystemProperties.user.verificationCodeExpiryMinutes 决定（默认 10min）
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationCodeService {

    private static final String KEY_PREFIX = "email_verify_code:";

    private static final String F_CODE = "code";
    private static final String F_CODE_EXPIRES_AT = "codeExpiresAt";
    private static final String F_ATTEMPTS_LEFT = "attemptsLeft";
    private static final String F_RESEND_COUNT = "resendCount";
    private static final String F_LAST_RESEND_AT = "lastResendAt";

    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RESEND_COUNT = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final SystemProperties systemProperties;

    // ========== 对外 API ==========

    /**
     * 判断指定 email 是否存在有效验证码记录。
     */
    public boolean exists(String email) {
        Boolean hasKey = stringRedisTemplate.hasKey(key(email));
        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * 首次签发验证码（注册场景）。
     * <p>
     * 会覆盖已有记录。返回生成的 6 位数字验证码。
     */
    public String issue(String email) {
        String code = generateCode();
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expirySeconds();

        Map<String, String> fields = new HashMap<>();
        fields.put(F_CODE, code);
        fields.put(F_CODE_EXPIRES_AT, String.valueOf(expiresAt));
        fields.put(F_ATTEMPTS_LEFT, String.valueOf(MAX_ATTEMPTS));
        fields.put(F_RESEND_COUNT, "0");
        fields.put(F_LAST_RESEND_AT, String.valueOf(now));

        String k = key(email);
        stringRedisTemplate.opsForHash().putAll(k, fields);
        stringRedisTemplate.expire(k, Duration.ofSeconds(expirySeconds()));
        log.info("签发验证码成功: email={}", email);
        return code;
    }

    /**
     * 重发验证码（用户点击"重新发送"）。
     * <p>
     * 规则：
     * <ul>
     *   <li>距离上次 lastResendAt 不足 sendInterval → 抛 USER_VERIFICATION_CODE_SEND_TOO_FREQUENT</li>
     *   <li>resendCount 达到 {@link #MAX_RESEND_COUNT} → 抛 USER_VERIFICATION_CODE_SEND_TOO_FREQUENT</li>
     *   <li>否则：生成新 code、重置 attemptsLeft、resendCount+1、刷新 TTL</li>
     * </ul>
     */
    public String resend(String email) {
        String k = key(email);
        Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(k);
        long now = Instant.now().getEpochSecond();

        int resendCount = 0;
        if (!raw.isEmpty()) {
            long lastResendAt = parseLong(raw.get(F_LAST_RESEND_AT), 0);
            int sendIntervalSec = systemProperties.getUser().getVerificationCodeSendIntervalSeconds();
            if (now - lastResendAt < sendIntervalSec) {
                throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
            }
            resendCount = parseInt(raw.get(F_RESEND_COUNT), 0);
            if (resendCount >= MAX_RESEND_COUNT) {
                throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
            }
        }

        String code = generateCode();
        long expiresAt = now + expirySeconds();

        Map<String, String> fields = new HashMap<>();
        fields.put(F_CODE, code);
        fields.put(F_CODE_EXPIRES_AT, String.valueOf(expiresAt));
        fields.put(F_ATTEMPTS_LEFT, String.valueOf(MAX_ATTEMPTS));
        fields.put(F_RESEND_COUNT, String.valueOf(resendCount + 1));
        fields.put(F_LAST_RESEND_AT, String.valueOf(now));

        stringRedisTemplate.opsForHash().putAll(k, fields);
        stringRedisTemplate.expire(k, Duration.ofSeconds(expirySeconds()));
        log.info("重发验证码成功: email={}, resendCount={}", email, resendCount + 1);
        return code;
    }

    /**
     * 校验验证码。
     * <ul>
     *   <li>记录不存在 / 已过期 → 抛 USER_VERIFICATION_CODE_NOT_FOUND / EXPIRED</li>
     *   <li>匹配失败且 attemptsLeft &gt; 1 → 递减 attemptsLeft，抛 INVALID</li>
     *   <li>匹配失败且 attemptsLeft == 1 → 删除 key，抛 ATTEMPTS_EXCEEDED</li>
     *   <li>匹配成功 → 删除 key，返回</li>
     * </ul>
     */
    public void verify(String email, String inputCode) {
        String k = key(email);
        List<Object> values = stringRedisTemplate.opsForHash()
                .multiGet(k, List.of(F_CODE, F_CODE_EXPIRES_AT, F_ATTEMPTS_LEFT));
        Object codeObj = values.get(0);
        if (codeObj == null) {
            throw StatusCode.USER_VERIFICATION_CODE_NOT_FOUND.exception();
        }
        long expiresAt = parseLong(values.get(1), 0);
        if (Instant.now().getEpochSecond() >= expiresAt) {
            stringRedisTemplate.delete(k);
            throw StatusCode.USER_VERIFICATION_CODE_EXPIRED.exception();
        }

        String expected = codeObj.toString();
        if (expected.equals(inputCode)) {
            stringRedisTemplate.delete(k);
            return;
        }

        int attemptsLeft = parseInt(values.get(2), 0);
        if (attemptsLeft <= 1) {
            stringRedisTemplate.delete(k);
            throw StatusCode.USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED.exception();
        }
        stringRedisTemplate.opsForHash().increment(k, F_ATTEMPTS_LEFT, -1);
        throw StatusCode.USER_VERIFICATION_CODE_INVALID.exception();
    }

    /**
     * 返回距离可重发的剩余秒数（0 表示立即可重发）。
     */
    public long secondsUntilResendAvailable(String email) {
        Object lastResend = stringRedisTemplate.opsForHash().get(key(email), F_LAST_RESEND_AT);
        if (lastResend == null) return 0;
        long last = parseLong(lastResend, 0);
        long intervalSec = systemProperties.getUser().getVerificationCodeSendIntervalSeconds();
        long remaining = (last + intervalSec) - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    // ========== 工具 ==========

    private static String key(String email) {
        return KEY_PREFIX + email;
    }

    private int expirySeconds() {
        return systemProperties.getUser().getVerificationCodeExpiryMinutes() * 60;
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
}
