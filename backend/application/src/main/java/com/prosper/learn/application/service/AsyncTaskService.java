package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 异步任务服务
 * 支持启动后台任务并通过 Redis 存储任务状态和结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TASK_KEY_PREFIX = "async_task:";
    private static final Duration TASK_TTL = Duration.ofHours(24);

    /**
     * 任务状态
     */
    public enum TaskStatus {
        RUNNING, COMPLETED, FAILED
    }

    /**
     * 生成任务ID
     */
    public String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 初始化任务状态为 RUNNING
     */
    public void initTask(String taskId) {
        saveTaskStatus(taskId, TaskStatus.RUNNING, null, null);
    }

    /**
     * 更新任务进度（运行中状态）
     */
    public void updateProgress(String taskId, Object progress) {
        saveTaskStatus(taskId, TaskStatus.RUNNING, progress, null);
    }

    /**
     * 异步执行任务（带进度回调）
     * 注意：此方法会在新线程中执行，需要传递语言上下文
     *
     * @param taskId 任务ID
     * @param language 语言上下文（从调用方获取 DataSourceContextHolder.getLanguage()）
     * @param task 任务执行逻辑
     */
    @Async
    public void runAsyncWithProgress(String taskId, String language, Consumer<Consumer<Object>> task) {
        DataSourceContextHolder.setLanguage(language);
        try {
            // 进度回调
            Consumer<Object> progressCallback = progress -> updateProgress(taskId, progress);

            // 执行任务
            task.accept(progressCallback);

            log.info("[{}] 异步任务 {} 执行成功", language, taskId);

        } catch (Exception e) {
            // 保存失败结果
            saveTaskStatus(taskId, TaskStatus.FAILED, null, e.getMessage());
            log.error("[{}] 异步任务 {} 执行失败", language, taskId, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 标记任务完成
     */
    public void completeTask(String taskId, Object result) {
        saveTaskStatus(taskId, TaskStatus.COMPLETED, result, null);
    }

    /**
     * 标记任务失败
     */
    public void failTask(String taskId, String error) {
        saveTaskStatus(taskId, TaskStatus.FAILED, null, error);
    }

    /**
     * 查询任务状态
     */
    public Map<String, Object> getTaskResult(String taskId) {
        String key = RedisKeyPrefix.prefix(TASK_KEY_PREFIX + taskId);
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            return result;
        } catch (JsonProcessingException e) {
            log.error("解析任务结果失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 保存任务状态到 Redis
     */
    private void saveTaskStatus(String taskId, TaskStatus status, Object result, String error) {
        String key = RedisKeyPrefix.prefix(TASK_KEY_PREFIX + taskId);

        Map<String, Object> taskResult = new java.util.HashMap<>();
        taskResult.put("taskId", taskId);
        taskResult.put("status", status.name());
        if (result != null) {
            taskResult.put("result", result);
        }
        if (error != null) {
            taskResult.put("error", error);
        }

        try {
            String json = objectMapper.writeValueAsString(taskResult);
            redisTemplate.opsForValue().set(key, json, TASK_TTL);
        } catch (JsonProcessingException e) {
            log.error("保存任务状态失败: {}", taskId, e);
        }
    }
}
