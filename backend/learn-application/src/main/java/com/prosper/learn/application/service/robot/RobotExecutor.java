package com.prosper.learn.application.service.robot;

import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.service.MemoryCardDeckService;
import com.prosper.learn.application.service.PostService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Robot 执行器
 *
 * 轮询 Redis ZSET 队列：
 * - 取队首任务ID（N:nodeId 或 C:postId）
 * - 幂等检查：若已有相关内容则跳过
 * - 调用生成服务；成功则移除任务；失败则停止当前轮次，等待下次定时器重新处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotExecutor {

    private final PostQueueService queueService;
    private final PostGenerationService generationService;
    private final PostDataService postDataService;
    private final PostService postService;
    private final UserDataService userDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDeckService memoryCardDeckService;
    private final SystemProperties systemProperties;

    @Scheduled(fixedDelayString = "#{systemProperties.robot.pollIntervalSec * 1000}")
    public void poll() {
        log.info("Robot 轮询开始");
        if (!systemProperties.getRobot().isEnabled()) return;
        if (queueService.isPaused()) {
            log.info("Robot 队列已暂停，跳过轮询");
            return;
        }

        long aiUserId = systemProperties.getRobot().getAiUserId();
        int processedCount = 0;

        // 循环执行直到队列为空
        while (true) {
            // 在每次循环时检查暂停状态，实时响应暂停操作
            if (queueService.isPaused()) {
                log.info("Robot 队列执行中被暂停，停止轮询，已处理 {} 个任务", processedCount);
                break;
            }

            String taskId = queueService.peek();
            if (taskId == null) {
                log.info("Robot 轮询结束，已处理 {} 个任务", processedCount);
                break;
            }

            boolean success = false;
            Exception lastException = null;

            // 尝试执行任务，失败时立即重试一次
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    if (taskId.startsWith("N:")) {
                        // 节点内容生成任务
                        handleNodeTask(taskId, aiUserId);
                        queueService.recordCompletion();
                    } else if (taskId.startsWith("C:")) {
                        // 记忆卡片生成任务
                        handleMemoryCardTask(taskId, aiUserId);
                        queueService.recordCompletion();
                    } else {
                        log.warn("Robot 未知任务格式: {}，从队列移除", taskId);
                        queueService.remove(taskId);
                    }
                    success = true;
                    break; // 成功则跳出重试循环
                } catch (Exception e) {
                    lastException = e;
                    if (attempt == 0) {
                        log.warn("Robot 任务 {} 第一次执行失败，立即重试", taskId);
                    }
                }
            }

            if (success) {
                processedCount++;
                queueService.resetFailures(); // 成功则重置连续失败计数
            } else {
                // 两次尝试都失败，移到队列尾部
                log.error("Robot 任务 {} 两次尝试均失败，移至队列尾部", taskId, lastException);
                queueService.moveToEnd(taskId);

                // 记录连续失败
                int consecutiveFailures = queueService.recordFailure();
                log.warn("Robot 连续失败次数: {}", consecutiveFailures);

                // 检查是否应该自动暂停
                if (queueService.shouldAutoPause()) {
                    log.error("Robot 连续失败次数达到上限({}次)，自动暂停队列执行", consecutiveFailures);
                    queueService.pause();
                    break;
                }
            }
        }
    }

    /**
     * 处理节点内容生成任务
     */
    private void handleNodeTask(String taskId, long aiUserId) {
        // 解析任务格式：N:{nodeId}:{contentType}:{recursive}:{deleteExisting}
        String[] parts = taskId.split(":");
        if (parts.length != 5) {
            log.warn("Robot 无效任务格式: {}，从队列移除", taskId);
            queueService.remove(taskId);
            return;
        }

        long nodeId = Long.parseLong(parts[1]);
        String contentType = parts[2];
        boolean recursive = Boolean.parseBoolean(parts[3]);
        boolean deleteExisting = Boolean.parseBoolean(parts[4]);

        if (deleteExisting && postDataService.existPost(nodeId, aiUserId)) {
            log.info("Robot 节点 {} 已存在 AI 帖子，重新生成前先软删除现有帖子", nodeId);

            // 查找并软删除现有的AI帖子
            List<PostDO> existingPosts = postDataService.getListByNodeAndCreator(nodeId, aiUserId);
            for (PostDO post : existingPosts) {
                if (post.getState() != Enums.ContentState.BANNED.value()) {
                    postService.deletePost(post.getId(), userDataService.getById(aiUserId));
                    log.info("Robot 软删除节点 {} 的现有 AI 帖子 {}", nodeId, post.getId());
                }
            }
        }

        // 重新生成内容
        generationService.generateForNode(nodeId, contentType, recursive);
        queueService.remove(taskId);
        log.info("Robot 节点 {} 内容生成成功，contentType={}，recursive={}，deleteExisting={}",
            nodeId, contentType, recursive, deleteExisting);
    }

    /**
     * 处理记忆卡片生成任务
     */
    private void handleMemoryCardTask(String taskId, long aiUserId) {
        long postId = Long.parseLong(taskId.substring(2)); // 去掉 "C:" 前缀

        // 检查是否已存在AI生成的卡片组（查找所有状态）
        List<MemoryCardDeckDO> existingDecks =
            deckDataService.getListByPostAndCreatorAllStates(postId, aiUserId, 10);

        if (!existingDecks.isEmpty()) {
            // 废弃所有已存在的AI deck
            existingDecks.forEach(deck -> {
                if (deck.getState() == Enums.ContentState.SUBMITTED.value() || deck.getState() == Enums.ContentState.PUBLISHED.value()) {
                    memoryCardDeckService.ban(deck.getId(), aiUserId, "Discarding for AI regeneration");
                    log.info("Robot 废弃帖子 {} 的现有 AI 卡片组 {}", postId, deck.getId());
                }
            });
        }

        // 生成新的记忆卡片
        generateMemoryCardsForPost(postId, aiUserId);
        queueService.remove(taskId);
        log.info("Robot 帖子 {} 记忆卡片生成成功", postId);
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
