package com.twicemax.shared.infrastructure.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 *
 * 使用示例：
 * <pre>
 * @DistributedLock(key = "'memory:add_deck:' + #userId + ':' + #nodeId", waitTime = 5, leaseTime = 30)
 * public void addDeckToMemoryBank(Long userId, Long nodeId, ...) {
 *     // 业务逻辑
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的 key，支持 SpEL 表达式
     * 例如：'memory:add_deck:' + #userId + ':' + #nodeId
     */
    String key();

    /**
     * 等待锁的时间（秒）
     * 默认 5 秒
     */
    long waitTime() default 5;

    /**
     * 锁的自动释放时间（秒）
     * 默认 30 秒
     */
    long leaseTime() default 30;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
