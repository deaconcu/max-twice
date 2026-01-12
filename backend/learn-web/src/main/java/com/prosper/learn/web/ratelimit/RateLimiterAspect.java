package com.prosper.learn.web.ratelimit;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.web.util.IpUtils;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * 限流切面
 *
 * @author Claude Code
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private final ProxyManager<String> proxyManager;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    public RateLimiterAspect(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Around("@annotation(com.prosper.learn.web.ratelimit.RateLimit) || @within(com.prosper.learn.web.ratelimit.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 如果限流被禁用，直接放行
        if (!rateLimitEnabled) {
            return joinPoint.proceed();
        }

        // 明确获取注解：优先使用方法级别的注解，如果没有则使用类级别的注解
        RateLimit rateLimit = null;

        // 1. 先尝试从方法上获取（优先级最高）
        try {
            rateLimit = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(RateLimit.class);
        } catch (Exception e) {
            log.debug("Failed to get method annotation", e);
        }

        // 2. 如果方法上没有，再从类上获取
        if (rateLimit == null) {
            Class<?> targetClass = joinPoint.getTarget().getClass();
            rateLimit = targetClass.getAnnotation(RateLimit.class);
        }

        // 3. 如果类和方法上都没有注解，直接放行（理论上不会发生）
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 1. 白名单角色检查
        if (shouldSkipRateLimit(rateLimit.skipForRoles())) {
            return joinPoint.proceed();
        }

        // 2. 生成 Key 和配置
        String key = generateKey(joinPoint, rateLimit);
        log.debug("Rate limit check for key: {}", key);

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(
                rateLimit.capacity(),
                Duration.of(rateLimit.refillPeriod(), rateLimit.refillUnit().toChronoUnit())
            ))
            .build();

        Bucket bucket = proxyManager.builder().build(key, configuration);

        // 3. 尝试消费令牌
        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            log.warn("Rate limit exceeded - key: {}, method: {}", key, joinPoint.getSignature().toShortString());
            throw StatusCode.RATE_LIMIT_EXCEEDED.exception();
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodSignature = joinPoint.getSignature().toLongString();

        switch (rateLimit.limitType()) {
            case USER:
                if (!StpUtil.isLogin()) {
                    throw new NotLoginException("此操作需要用户登录", StpUtil.getLoginType(), NotLoginException.NOT_TOKEN);
                }
                return "rate_limit:user:" + StpUtil.getLoginIdAsString() + ":" + methodSignature;

            case IP:
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes == null) {
                    throw StatusCode.RATE_LIMIT_CONFIG_ERROR.exception("IP 限流类型只能用于 HTTP 请求上下文");
                }
                HttpServletRequest request = attributes.getRequest();
                String ip = IpUtils.getIpAddress(request);
                if (ip == null) {
                    ip = "unknown";
                }
                return "rate_limit:ip:" + ip + ":" + methodSignature;

            case GLOBAL:
            default:
                return "rate_limit:global:" + methodSignature;
        }
    }

    private boolean shouldSkipRateLimit(String[] skipRoles) {
        if (!StpUtil.isLogin() || skipRoles.length == 0) {
            return false;
        }
        boolean shouldSkip = StpUtil.hasRoleOr(skipRoles);
        if (shouldSkip) {
            log.debug("Rate limit skipped for user '{}' with roles.", StpUtil.getLoginId());
        }
        return shouldSkip;
    }
}
