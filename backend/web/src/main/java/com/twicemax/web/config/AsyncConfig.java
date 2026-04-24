package com.twicemax.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局 @Async 线程池配置。
 * <p>
 * 不配的话 Spring Boot 3 会退化到 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
 * —— 每次新建线程、无上限，高并发下会把 JVM 线程数打爆并 OOM。
 * <p>
 * 当前策略：一个全局有界池，所有 @Async 共享。等真出现某类任务挤占另一类的情况
 * （例如 AI 生成把邮件发送堵住），再按场景拆多池 + {@code @Async("poolName")} 指定。
 * <p>
 * 拒绝策略选 CallerRunsPolicy：队列满了用调用方线程同步执行，请求会变慢但不丢任务，
 * 天然对上游形成反压，避免丢邮件、丢索引同步这类不能丢的副作用。
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关停：等在跑的任务完成再退出，避免邮件发一半 / 索引同步半截
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("async task failed: method={}", method.getName(), ex);
    }
}
