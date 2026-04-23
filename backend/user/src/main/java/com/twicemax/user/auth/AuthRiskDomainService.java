package com.twicemax.user.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证风险控制领域服务
 * <p>
 * 职责边界（严格限定）：
 * <ul>
 *   <li>按 IP 记录认证失败次数</li>
 *   <li>基于失败次数判断是否需要强制 Turnstile 人机验证</li>
 *   <li>成功后清零失败计数</li>
 * </ul>
 * <p>
 * 不负责：具体的 Turnstile 校验（见 TurnstileService）、OTP 编码/会话（见 OtpCodeService / OtpSessionService）、
 * 认证流程编排（见 application.AuthService）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRiskDomainService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:ip:";
    private static final int LOGIN_FAIL_MAX_ATTEMPTS = 3;
    private static final Duration LOGIN_FAIL_EXPIRE_TIME = Duration.ofMinutes(15);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断当前 IP 是否需要强制完成 Turnstile 验证。
     */
    public boolean isCaptchaRequired(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        try {
            return Integer.parseInt(value) >= LOGIN_FAIL_MAX_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 记录一次认证失败。达到阈值后 {@link #isCaptchaRequired} 返回 true。
     */
    public void recordFailure(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, LOGIN_FAIL_EXPIRE_TIME);
        }
        log.debug("IP {} 认证失败次数: {}", ip, count);
    }

    /**
     * 认证成功后清除失败计数。
     */
    public void clearFailures(String ip) {
        String key = LOGIN_FAIL_KEY_PREFIX + ip;
        stringRedisTemplate.delete(key);
    }
}
