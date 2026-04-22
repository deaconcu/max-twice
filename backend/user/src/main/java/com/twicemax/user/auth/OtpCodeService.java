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
 * OTP 验证码服务 —— 按 scene 管理验证码、尝试次数、重发限频。
 * <p>
 * 不同 scene 使用独立 Redis key 前缀，互不影响。
 * <p>
 * Redis 结构：
 * <pre>
 *   Key:   otp:code:{scene}:{identifier}
 *   Type:  Hash
 *   Field: code / codeExpiresAt / attemptsLeft / resendCount / lastResendAt
 *   TTL:   由 SystemProperties.user.verificationCodeExpiryMinutes 决定（默认 10min）
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCodeService {

    private static final String KEY_PREFIX = "otp:code:";

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
     * 判断指定 scene + identifier 是否存在有效验证码。
     */
    public boolean exists(OtpScene scene, String identifier) {
        Boolean hasKey = stringRedisTemplate.hasKey(key(scene, identifier));
        return Boolean.TRUE.equals(hasKey);
    }

    /**
     * 首次签发验证码（会覆盖已有记录）。返回 6 位数字验证码。
     */
    public String issue(OtpScene scene, String identifier) {
        String code = generateCode();
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + expirySeconds();

        Map<String, String> fields = new HashMap<>();
        fields.put(F_CODE, code);
        fields.put(F_CODE_EXPIRES_AT, String.valueOf(expiresAt));
        fields.put(F_ATTEMPTS_LEFT, String.valueOf(MAX_ATTEMPTS));
        fields.put(F_RESEND_COUNT, "0");
        fields.put(F_LAST_RESEND_AT, String.valueOf(now));

        String k = key(scene, identifier);
        stringRedisTemplate.opsForHash().putAll(k, fields);
        stringRedisTemplate.expire(k, Duration.ofSeconds(expirySeconds()));
        log.info("签发OTP: scene={}, identifier={}", scene, identifier);
        return code;
    }

    /**
     * 重发验证码。触发频控时抛 USER_VERIFICATION_CODE_SEND_TOO_FREQUENT。
     */
    public String resend(OtpScene scene, String identifier) {
        String k = key(scene, identifier);
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
        log.info("重发OTP: scene={}, identifier={}, resendCount={}", scene, identifier, resendCount + 1);
        return code;
    }

    /**
     * 校验验证码。
     * <p>
     * 行为：
     * <ul>
     *   <li>keepOnSuccess=false（默认）：匹配成功后删除 key</li>
     *   <li>keepOnSuccess=true：匹配成功后保留 key（供后续流程使用，例如密码重置需要保留到 confirm 阶段）</li>
     * </ul>
     * 匹配失败且 attemptsLeft &gt; 1：递减，抛 INVALID；
     * 匹配失败且 attemptsLeft == 1：删除 code 相关字段，抛 ATTEMPTS_EXCEEDED。
     */
    public void verify(OtpScene scene, String identifier, String inputCode, boolean keepOnSuccess) {
        String k = key(scene, identifier);
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
            if (!keepOnSuccess) {
                stringRedisTemplate.delete(k);
            }
            return;
        }

        int attemptsLeft = parseInt(values.get(2), 0);
        if (attemptsLeft <= 1) {
            // 清掉 code 字段（保留 key 让调用方决定是否继续）
            stringRedisTemplate.opsForHash().delete(k, F_CODE, F_CODE_EXPIRES_AT, F_ATTEMPTS_LEFT);
            throw StatusCode.USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED.exception();
        }
        stringRedisTemplate.opsForHash().increment(k, F_ATTEMPTS_LEFT, -1);
        throw StatusCode.USER_VERIFICATION_CODE_INVALID.exception();
    }

    /**
     * 主动删除验证码（例如流程确认后清理残留）。
     */
    public void invalidate(OtpScene scene, String identifier) {
        stringRedisTemplate.delete(key(scene, identifier));
    }

    /**
     * 返回距离可重发的剩余秒数（0 表示立即可重发）。
     */
    public long secondsUntilResendAvailable(OtpScene scene, String identifier) {
        Object lastResend = stringRedisTemplate.opsForHash()
                .get(key(scene, identifier), F_LAST_RESEND_AT);
        if (lastResend == null) return 0;
        long last = parseLong(lastResend, 0);
        long intervalSec = systemProperties.getUser().getVerificationCodeSendIntervalSeconds();
        long remaining = (last + intervalSec) - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    // ========== 工具 ==========

    private static String key(OtpScene scene, String identifier) {
        return KEY_PREFIX + scene.key() + ":" + identifier;
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
