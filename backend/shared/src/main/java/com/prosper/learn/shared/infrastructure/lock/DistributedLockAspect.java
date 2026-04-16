package com.prosper.learn.shared.infrastructure.lock;

import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 分布式锁 AOP 切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 解析锁的 key（支持 SpEL 表达式）
        String lockKey = parseKey(distributedLock.key(), joinPoint);
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁
            boolean acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("分布式锁 获取失败: lockKey={}", lockKey);
                throw StatusCode.SYSTEM_ERROR.exception();
            }

            log.debug("分布式锁 获取成功: lockKey={}", lockKey);

            // 执行业务方法
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式锁 获取时线程中断: lockKey={}", lockKey, e);
            throw StatusCode.SYSTEM_ERROR.exception();
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("分布式锁 释放成功: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 解析 SpEL 表达式
     */
    private String parseKey(String key, ProceedingJoinPoint joinPoint) {
        // 如果不包含表达式，直接返回
        if (!key.contains("#")) {
            return key;
        }

        // 获取方法参数名和参数值
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = discoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        // 构建 SpEL 上下文
        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        // 解析表达式
        Expression expression = parser.parseExpression(key);
        return expression.getValue(context, String.class);
    }
}
