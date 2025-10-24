package com.prosper.learn.domain.service.autoauthor;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.data.PostDataService;
import com.prosper.learn.domain.service.data.MemoryCardDeckDataService;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.domain.service.business.MemoryCardDeckService;
import com.prosper.learn.dto.request.CreateDeckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import com.prosper.learn.persistence.dataobject.PostDO;


/**
 * AutoAuthor 执行器
 *
 * 轮询 Redis ZSET 队列：
 * - 取队首任务ID（N:nodeId 或 C:postId）
 * - 幂等检查：若已有相关内容则跳过
 * - 调用生成服务；成功则移除任务；失败则停止当前轮次，等待下次定时器重新处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAuthorExecutor {

    private final AutoAuthorQueueService queueService;
    private final AutoAuthorGenerationService generationService;
    private final PostDataService postDataService;
    private final PostService postService;
    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDeckService memoryCardDeckService;
    private final SystemProperties systemProperties;

    @Scheduled(fixedDelayString = "#{systemProperties.autoAuthor.pollIntervalSec * 1000}")
    public void poll() {
        log.info("Auto Author Poll Start");
        if (!systemProperties.getAutoAuthor().isEnabled()) return;

        long aiUserId = systemProperties.getAutoAuthor().getAiUserId();
        int processedCount = 0;

        // 循环执行直到队列为空
        while (true) {
            String taskId = queueService.peek();
            if (taskId == null) {
                log.info("Auto Author Poll End - processed {} tasks", processedCount);
                break;
            }

            try {
                if (taskId.startsWith("N:")) {
                    // 节点内容生成任务
                    handleNodeTask(taskId, aiUserId);
                } else if (taskId.startsWith("C:")) {
                    // 记忆卡片生成任务
                    handleMemoryCardTask(taskId, aiUserId);
                } else {
                    log.warn("Unknown task format: {}, removing from queue", taskId);
                    queueService.remove(taskId);
                }
                processedCount++;
            } catch (Exception e) {
                log.error("auto-author failed for task {}, stopping poll cycle", taskId, e);
                break;
            }
        }
    }

    /**
     * 处理节点内容生成任务
     */
    private void handleNodeTask(String taskId, long aiUserId) {
        long nodeId = Long.parseLong(taskId.substring(2)); // 去掉 "N:" 前缀

        if (postDataService.existPost(nodeId, aiUserId)) {
            log.info("Node {} already has AI post, soft deleting existing post before regeneration", nodeId);

            // 查找并软删除现有的AI帖子
            List<PostDO> existingPosts = postDataService.getListByNodeAndCreator(nodeId, aiUserId);
            for (PostDO post : existingPosts) {
                if (post.getState() != Enums.ContentState.BANNED.value()) {
                    postService.deletePost(post.getId());
                    log.info("Soft deleted existing AI post {} for node {}", post.getId(), nodeId);
                }
            }
        }

        // 重新生成内容
        generationService.generateForNode(nodeId);
        queueService.remove(taskId);
        log.info("Successfully generated/updated content for node {}", nodeId);
    }

    /**
     * 处理记忆卡片生成任务
     */
    private void handleMemoryCardTask(String taskId, long aiUserId) {
        long postId = Long.parseLong(taskId.substring(2)); // 去掉 "C:" 前缀

        // 检查是否已存在AI生成的卡片组（查找所有状态）
        List<com.prosper.learn.persistence.dataobject.MemoryCardDeckDO> existingDecks =
            deckDataService.getListByPostAndCreatorAllStates(postId, aiUserId, 10);

        if (!existingDecks.isEmpty()) {
            // 废弃所有已存在的AI deck
            existingDecks.forEach(deck -> {
                if (deck.getState() == Enums.ContentState.SUBMITTED.value() || deck.getState() == Enums.ContentState.PUBLISHED.value()) {
                    memoryCardDeckService.ban(deck.getId(), aiUserId, "Discarding for AI regeneration");
                    log.info("Discarded existing AI deck {} for post {}", deck.getId(), postId);
                }
            });
        }

        // 生成新的记忆卡片
        generateMemoryCardsForPost(postId, aiUserId);
        queueService.remove(taskId);
        log.info("Successfully generated memory cards for post {}", postId);
    }

    /**
     * 为帖子生成记忆卡片
     */
    private void generateMemoryCardsForPost(long postId, long aiUserId) {
        // 获取帖子内容
        var post = postDataService.validateAndGet(postId);

        // 调用AI生成记忆卡片（如果失败会抛出异常，任务会重试）
        List<CreateDeckRequest.CardInfo> cards = generationService.generateMemoryCardsForContent(post.getContent());

        // 直接调用 createMemoryCardsForPost 方法
        generationService.createMemoryCardsForPost(postId, aiUserId, cards);
    }
}
