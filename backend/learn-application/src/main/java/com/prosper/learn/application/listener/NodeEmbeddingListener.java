package com.prosper.learn.application.listener;

import com.prosper.learn.infrastructure.embedding.EmbeddingService;
import com.prosper.learn.infrastructure.embedding.MilvusService;
import com.prosper.learn.shared.domain.event.content.lifecycle.NodeCreatedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.NodeUpdatedEvent;
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

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    /**
     * 节点创建时生成embedding
     */
    @EventListener
    @Async
    public void onNodeCreated(NodeCreatedEvent event) {
        try {
            generateAndStoreEmbedding(event.getId(), event.getName(), event.getDescription());
            log.info("Generated embedding for new node: {}", event.getId());
        } catch (Exception e) {
            log.error("Failed to generate embedding for node: {}", event.getId(), e);
            // 失败不影响节点创建，通过补偿机制处理
        }
    }

    /**
     * 节点更新时重新生成embedding
     */
    @EventListener
    @Async
    public void onNodeUpdated(NodeUpdatedEvent event) {
        try {
            generateAndStoreEmbedding(event.getId(), event.getName(), event.getDescription());
            log.info("Regenerated embedding for updated node: {}", event.getId());
        } catch (Exception e) {
            log.error("Failed to regenerate embedding for node: {}", event.getId(), e);
        }
    }

    /**
     * 生成并存储embedding
     */
    private void generateAndStoreEmbedding(Long id, String name, String description) {
        // 拼接文本：标题 + 描述
        String text = name + "\n\n" + description;

        // 生成embedding
        float[] embedding = embeddingService.generateEmbedding(text);

        // 存储到Milvus
        milvusService.upsert(id, embedding);
    }
}
