package com.prosper.learn.application.service;

import com.prosper.learn.infrastructure.datasource.DataSourceContextHolder;
import com.prosper.learn.infrastructure.embedding.EmbeddingService;
import com.prosper.learn.infrastructure.embedding.MilvusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 节点向量服务
 * 负责异步处理节点的向量生成和存储
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NodeEmbeddingService {

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    /**
     * 异步生成并存储节点向量
     * @param language 语言上下文（从调用方获取）
     */
    @Async
    public void upsertAsync(Long nodeId, String name, String description, String language) {
        DataSourceContextHolder.setLanguage(language);
        try {
            String text = name + "\n\n" + (description != null ? description : "");
            float[] embedding = embeddingService.generateEmbedding(text);
            milvusService.upsert(nodeId, embedding);
            log.debug("[{}] 节点向量更新成功: nodeId={}", language, nodeId);
        } catch (Exception e) {
            log.error("[{}] 节点向量更新失败: nodeId={}", language, nodeId, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    /**
     * 异步删除节点向量
     * @param language 语言上下文（从调用方获取）
     */
    @Async
    public void deleteAsync(Long nodeId, String language) {
        DataSourceContextHolder.setLanguage(language);
        try {
            milvusService.delete(nodeId);
            log.debug("[{}] 节点向量删除成功: nodeId={}", language, nodeId);
        } catch (Exception e) {
            log.error("[{}] 节点向量删除失败: nodeId={}", language, nodeId, e);
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
