package com.prosper.learn.domain.service.autoauthor;

import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.data.PostDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AutoAuthor 执行器
 *
 * 轮询 Redis ZSET 队列：
 * - 取队首 nodeId
 * - 幂等：若该节点已有 AI 用户帖子（任意类型且未删除）则 ZREM 跳过
 * - 调用生成服务；成功 ZREM；失败休眠 retryDelaySec 原地重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAuthorExecutor {

    private final AutoAuthorQueueService queueService;
    private final AutoAuthorGenerationService generationService;
    private final PostDataService postDataService;
    private final SystemProperties systemProperties;

    @Scheduled(fixedDelayString = "#{systemProperties.autoAuthor.pollIntervalSec * 1000}")
    public void poll() {
        log.info("Auto Author Poll Start");
        if (!systemProperties.getAutoAuthor().isEnabled()) return;
        Long nodeId = queueService.peek();
        if (nodeId == null) return;
        long aiUserId = systemProperties.getAutoAuthor().getAiUserId();
        try {
            if (postDataService.existPost(nodeId, aiUserId)) {
                queueService.remove(nodeId);
                return;
            }
            generationService.generateForNode(nodeId);
            queueService.remove(nodeId);
        } catch (Exception e) {
            log.error("auto-author failed for node {}", nodeId, e);
            try {
                Thread.sleep(systemProperties.getAutoAuthor().getRetryDelaySec() * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
