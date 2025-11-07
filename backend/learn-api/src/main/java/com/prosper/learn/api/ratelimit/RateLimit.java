package com.prosper.learn.api.ratelimit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义注解，用于声明式地为接口方法提供速率限制
 *
 * 可以用于：
 * 1. 方法级别：只对该方法限流
 * 2. 类级别：对该类的所有方法应用相同的限流规则（方法级别的注解会覆盖类级别）
 *
 * @author Claude Code
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 令牌桶容量（窗口内的最大请求数）
     */
    long capacity();

    /**
     * 补充令牌的时间周期
     */
    long refillPeriod();

    /**
     * 补充令牌的时间单位，默认为秒
     */
    TimeUnit refillUnit() default TimeUnit.SECONDS;

    /**
     * 需要跳过限流的 Sa-Token 角色列表
     */
    String[] skipForRoles() default {};

    /**
     * 限流类型：按用户、按IP或全局
     */
    LimitType limitType() default LimitType.USER;
}
