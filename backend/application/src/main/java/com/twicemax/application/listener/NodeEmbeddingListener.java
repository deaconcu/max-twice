package com.twicemax.application.listener;

import com.twicemax.application.service.NodeEmbeddingService;
import com.twicemax.shared.domain.event.content.lifecycle.NodeCreatedEvent;
import com.twicemax.shared.domain.event.content.lifecycle.NodeUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 节点Embedding事件监听器
 * 负责在节点创建/更新时自动生成并存储embedding向量
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NodeEmbeddingListener {

    private final NodeEmbeddingService nodeEmbeddingService;

    /**
     * 节点创建时生成embedding
     */
    @EventListener
    @Async
    public void onNodeCreated(NodeCreatedEvent event) {
        nodeEmbeddingService.upsertAsync(event.getId(), event.getName(), event.getDescription(), event.getLanguage());
        log.info("[{}] 节点向量生成任务已提交: nodeId={}", event.getLanguage(), event.getId());
    }

    /**
     * 节点更新时重新生成embedding
     */
    @EventListener
    @Async
    public void onNodeUpdated(NodeUpdatedEvent event) {
        nodeEmbeddingService.upsertAsync(event.getId(), event.getName(), event.getDescription(), event.getLanguage());
        log.info("[{}] 节点向量更新任务已提交: nodeId={}", event.getLanguage(), event.getId());
    }
}
