# 基于 Bucket4j 的精细化接口限流方案 (V8 - 最终生产蓝图)

> **版本说明**: 此 V8 版本是经过多轮迭代和代码审查后的最终版本，是可直接用于生产的、完整、自包含的最终方案。

## 1. 核心设计：明确限流类型

为提升方案的灵活性和代码可读性，我们将在 `@RateLimit` 注解中引入一个 `LimitType` 枚举，用于显式声明限流策略。

- **`USER`**: 按用户ID限流。此策略会强制要求用户已登录，若未登录，将直接拒绝请求并返回 HTTP 401 Unauthorized。
- **`IP`**: 按请求来源IP地址限流。在非HTTP请求上下文中使用此类型会抛出 `IllegalStateException`，遵循快速失败原则。
- **`GLOBAL`**: 全局限流。所有请求共享同一个令牌桶，适用于保护特别消耗资源的全局性接口。

## 2. 最终实现代码

### 2.1. 枚举 `LimitType`
```java
package com.prosper.learn.api.ratelimit;

public enum LimitType {
    USER,
    IP,
    GLOBAL
}
```

### 2.2. `@RateLimit` 注解 (V8)
```java
package com.prosper.learn.api.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    long capacity();
    long refillPeriod();
    TimeUnit refillUnit() default TimeUnit.SECONDS;
    String[] skipForRoles() default {};
    LimitType limitType() default LimitType.USER;
}
```

### 2.3. AOP 切面 `RateLimiterAspect` (V8)
```java
package com.prosper.learn.api.ratelimit;

import cn.dev33.satoken.stp.StpUtil;
import com.bucket4j.*;
import com.bucket4j.distributed.proxy.ProxyManager;
import com.prosper.learn.api.exception.RateLimitException;
import com.prosper.learn.api.exception.UnauthenticatedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    private final ProxyManager<String> proxyManager;

    public RateLimiterAspect(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (shouldSkipRateLimit(rateLimit.skipForRoles())) {
            return joinPoint.proceed();
        }

        String key = generateKey(joinPoint, rateLimit);

        BucketConfiguration configuration = BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(
                rateLimit.capacity(),
                Duration.of(rateLimit.refillPeriod(), rateLimit.refillUnit().toChronoUnit())
            ))
            .build();

        Bucket bucket = proxyManager.builder().build(key, configuration);

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            log.warn("Rate limit exceeded - key: {}, method: {}", key, joinPoint.getSignature().toShortString());
            long retryAfterSeconds = rateLimit.refillUnit().toSeconds(rateLimit.refillPeriod());
            throw new RateLimitException("Too many requests", retryAfterSeconds);
        }
    }

    private String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodSignature = joinPoint.getSignature().toLongString();

        switch (rateLimit.limitType()) {
            case USER:
                if (!StpUtil.isLogin()) {
                    throw new UnauthenticatedAccessException("此操作需要用户登录。");
                }
                return "rate_limit:user:" + StpUtil.getLoginIdAsString() + ":" + methodSignature;

            case IP:
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes == null) {
                    throw new IllegalStateException("IP 限流类型只能用于 HTTP 请求上下文。");
                }
                HttpServletRequest request = attributes.getRequest();
                String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                                  .map(xff -> xff.split(",")[0].trim())
                                  .orElseGet(request::getRemoteAddr);
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
```

### 2.4. 异常定义
```java
// RateLimitException.java
package com.prosper.learn.api.exception;

public class RateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

// UnauthenticatedAccessException.java
package com.prosper.learn.api.exception;

public class UnauthenticatedAccessException extends RuntimeException {
    public UnauthenticatedAccessException(String message) {
        super(message);
    }
}
```

### 2.5. 全局异常处理器
```java
package com.prosper.learn.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleRateLimitException(RateLimitException ex, HttpServletResponse response) {
        long retryAfter = ex.getRetryAfterSeconds();
        response.addHeader("Retry-After", String.valueOf(retryAfter));

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("message", "访问过于频繁，请稍后再试");
        body.put("retryAfterSeconds", retryAfter);
        return body;
    }

    @ExceptionHandler(UnauthenticatedAccessException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthenticated(UnauthenticatedAccessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("message", ex.getMessage());
        return body;
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", "服务器内部配置错误: " + ex.getMessage());
        return body;
    }
}
```

## 3. 配置与依赖
### 3.1. Maven 依赖 (`pom.xml`)
```xml
<!-- sa-token-spring-boot3-starter -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>1.38.0</version>
</dependency>
<!-- bucket4j-jcache -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-jcache</artifactId>
    <version>8.9.0</version>
</dependency>
<!-- redisson-spring-boot-starter -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.2</version>
</dependency>
<!-- 其他 Spring Boot, AOP, Lombok 依赖 -->
```

### 3.2. 配置类 (`RateLimitConfig.java`)
```java
package com.prosper.learn.api.config;

import com.bucket4j.distributed.proxy.ProxyManager;
import com.bucket4j.grid.jcache.JCacheProxyManager;
import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        MutableConfiguration<String, byte[]> config = new MutableConfiguration<>();
        config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
        manager.createCache("rateLimitBuckets", RedissonConfiguration.fromConfig(config, redissonClient.getConfig()));
        return manager;
    }

    @Bean
    public ProxyManager<String> proxyManager(CacheManager cacheManager) {
        return new JCacheProxyManager<>(cacheManager.getCache("rateLimitBuckets"));
    }
}
```

## 4. 使用示例
```java
// 示例1：登录接口 - 按 IP 限流
@PostMapping("/api/v1/auth/login")
@RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) { /*...*/ }

// 示例2：用户资料修改 - 按 USER 限流
@PutMapping("/api/v1/user/profile")
@RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public ResponseEntity<Void> updateProfile(@RequestBody ProfileRequest request) { /*...*/ }

// 示例3：敏感全局操作 - GLOBAL 限流
@PostMapping("/api/v1/admin/reset-cache")
@RateLimit(capacity = 1, refillPeriod = 10, refillUnit = TimeUnit.MINUTES, limitType = LimitType.GLOBAL, skipForRoles = {"SUPER_ADMIN"})
public ResponseEntity<Void> resetCache() { /*...*/ }
```

## 5. 测试与验证
### 5.1. 集成测试示例
```java
package com.prosper.learn.api.ratelimit;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RateLimiterAspectTest {

    @Container
    static final RedisContainer REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Service
    public static class TestService {
        @RateLimit(capacity = 2, refillPeriod = 10, refillUnit = TimeUnit.SECONDS, limitType = LimitType.USER)
        public void limitedMethodForUser() {}
    }

    @Autowired
    private TestService testService;

    @Test
    void shouldBlockUserAfterExceedingLimit() {
        StpUtil.login(10001L);
        try {
            assertDoesNotThrow(() -> testService.limitedMethodForUser());
            assertDoesNotThrow(() -> testService.limitedMethodForUser());
            assertThrows(RateLimitException.class, () -> testService.limitedMethodForUser());
        } finally {
            StpUtil.logout();
        }
    }
}
```
(后续章节如 FAQ, 监控等保持不变)
