package com.twicemax.user.auth;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 认证风险控制领域服务
 * <p>
 * 职责边界（严格限定）：
 * <ul>
 *   <li>按 IP 记录认证失败次数，判断是否需要强制 Turnstile</li>
 *   <li>邮件发送的日配额：单邮箱 / 单 IP 每日上限（防邮件轰炸）</li>
 * </ul>
 * <p>
 * 不负责：具体的 Turnstile 校验（见 TurnstileService）、OTP 编码/会话（见 OtpCodeService / OtpSessionService）、
 * 认证流程编排（见 application.AuthService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRiskDomainService {

    private static final String LOGIN_FAIL_IP_KEY_PREFIX = "login:fail:ip:";
    private static final String LOGIN_FAIL_EMAIL_KEY_PREFIX = "login:fail:email:";
    private static final int LOGIN_FAIL_IP_THRESHOLD = 10;     // 粗粒度：对 NAT 出口宽松，抓同 IP 大量遍历
    private static final int LOGIN_FAIL_EMAIL_THRESHOLD = 3;   // 细粒度：抓分布式 IP 打同账户
    private static final Duration LOGIN_FAIL_EXPIRE_TIME = Duration.ofMinutes(15);

    // 邮件发送日配额：key 含 yyyyMMdd，跨天天然切换；TTL 48h 只是兜底清理
    private static final String EMAIL_QUOTA_IP_KEY_PREFIX = "email:quota:ip:";
    private static final String EMAIL_QUOTA_EMAIL_KEY_PREFIX = "email:quota:email:";
    private static final int EMAIL_QUOTA_PER_IP_DAILY = 30;
    private static final int EMAIL_QUOTA_PER_EMAIL_DAILY = 10;
    private static final Duration EMAIL_QUOTA_TTL = Duration.ofHours(48);
    private static final DateTimeFormatter DAY_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断当前 IP 或邮箱是否需要强制完成 Turnstile 验证。
     * 任一维度超阈值即返回 true。
     */
    public boolean isCaptchaRequired(String ip, String email) {
        if (ip != null && !ip.isEmpty()
                && exceedsThreshold(LOGIN_FAIL_IP_KEY_PREFIX + ip, LOGIN_FAIL_IP_THRESHOLD)) {
            return true;
        }
        if (email != null && !email.isEmpty()
                && exceedsThreshold(LOGIN_FAIL_EMAIL_KEY_PREFIX + email, LOGIN_FAIL_EMAIL_THRESHOLD)) {
            return true;
        }
        return false;
    }

    /**
     * 记录一次认证失败（IP 和 email 各独立计数）。
     */
    public void recordFailure(String ip, String email) {
        if (ip != null && !ip.isEmpty()) {
            incWithTtl(LOGIN_FAIL_IP_KEY_PREFIX + ip);
        }
        if (email != null && !email.isEmpty()) {
            incWithTtl(LOGIN_FAIL_EMAIL_KEY_PREFIX + email);
        }
    }

    /**
     * 认证成功后清除失败计数（IP 和 email 都清）。
     */
    public void clearFailures(String ip, String email) {
        if (ip != null && !ip.isEmpty()) {
            stringRedisTemplate.delete(LOGIN_FAIL_IP_KEY_PREFIX + ip);
        }
        if (email != null && !email.isEmpty()) {
            stringRedisTemplate.delete(LOGIN_FAIL_EMAIL_KEY_PREFIX + email);
        }
    }

    private boolean exceedsThreshold(String key, int threshold) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) return false;
        try {
            return Integer.parseInt(value) >= threshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void incWithTtl(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, LOGIN_FAIL_EXPIRE_TIME);
        }
        log.debug("认证失败计数 key={} count={}", key, count);
    }

    /**
     * 校验并递增"邮件发送日配额"（同邮箱/同 IP 各独立计数）。
     * <p>
     * 在真正发邮件（或创建 OTP 导致发邮件）之前调用。超过任一维度上限时抛
     * {@link StatusCode#USER_VERIFICATION_CODE_SEND_TOO_FREQUENT}。
     * <p>
     * 实现细节：用 INCR 保证原子性；超限后计数仍会继续涨（但有 TTL 兜底），
     * 不影响功能，只是让攻击者无效请求也在惩罚自己的 key。
     */
    public void checkAndIncrementEmailQuota(String email, String ip) {
        String day = DAY_FMT.format(Instant.now());
        incAndCheck(EMAIL_QUOTA_EMAIL_KEY_PREFIX + email + ":" + day, EMAIL_QUOTA_PER_EMAIL_DAILY);
        if (ip != null && !ip.isEmpty()) {
            incAndCheck(EMAIL_QUOTA_IP_KEY_PREFIX + ip + ":" + day, EMAIL_QUOTA_PER_IP_DAILY);
        }
    }

    private void incAndCheck(String key, int limit) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, EMAIL_QUOTA_TTL);
        }
        if (count != null && count > limit) {
            throw StatusCode.USER_VERIFICATION_CODE_SEND_TOO_FREQUENT.exception();
        }
    }
}
