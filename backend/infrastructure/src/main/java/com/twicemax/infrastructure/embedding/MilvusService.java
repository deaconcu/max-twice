package com.twicemax.infrastructure.embedding;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;

/**
 * Milvus向量数据库服务
 * 使用 Milvus SDK 2.6.x API
 *
 * Collection 按语言分开：zh_nodes, en_nodes
 */
@Service
@Slf4j
public class MilvusService {

    private MilvusClientV2 milvusClient;
    private final Gson gson = new Gson();

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    @Value("${milvus.collection-name}")
    private String baseCollectionName;

    @Value("${milvus.vector-dim}")
    private int vectorDim;

    @Value("${milvus.enabled:true}")
    private boolean enabled;

    /**
     * 获取当前语言的 collection 名称
     */
    private String getCollectionName() {
        return DataSourceContextHolder.getLanguage() + "_" + baseCollectionName;
    }

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Milvus 未启用，跳过初始化");
            return;
        }

        try {
            // 创建连接配置
            ConnectConfig config = ConnectConfig.builder()
                    .uri("http://" + host + ":" + port)
                    .build();

            // 创建客户端
            milvusClient = new MilvusClientV2(config);
            log.info("Milvus 连接成功: {}:{}", host, port);

            // 为每种语言初始化 collection
            for (String lang : DataSourceContextHolder.SUPPORTED_LANGUAGES) {
                String collectionName = lang + "_" + baseCollectionName;
                initializeCollection(collectionName);
            }

        } catch (Exception e) {
            log.error("Milvus 初始化失败", e);
            throw new RuntimeException("Milvus 初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (milvusClient != null) {
            milvusClient.close();
            log.info("Milvus 客户端已关闭");
        }
    }

    private void initializeCollection(String collectionName) {
        try {
            // 检查 collection 是否存在
            boolean exists = milvusClient.hasCollection(
                    HasCollectionReq.builder()
                            .collectionName(collectionName)
                            .build()
            );

            if (exists) {
                log.info("Milvus 集合 '{}' 已存在", collectionName);
                return;
            }

            // 构建 schema
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                    .build();

            // ID 字段
            schema.addField(AddFieldReq.builder()
                    .fieldName("id")
                    .dataType(DataType.Int64)
                    .isPrimaryKey(true)
                    .build());

            // Embedding 向量字段
            schema.addField(AddFieldReq.builder()
                    .fieldName("embedding")
                    .dataType(DataType.FloatVector)
                    .dimension(vectorDim)
                    .build());

            // 创建 HNSW 索引
            List<IndexParam> indexes = new ArrayList<>();
            Map<String, Object> indexParams = new HashMap<>();
            indexParams.put("M", 16);
            indexParams.put("efConstruction", 256);

            indexes.add(IndexParam.builder()
                    .fieldName("embedding")
                    .indexType(IndexParam.IndexType.HNSW)
                    .metricType(IndexParam.MetricType.COSINE)
                    .extraParams(indexParams)
                    .build());

            // 创建 collection
            CreateCollectionReq request = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexes)
                    .build();

            milvusClient.createCollection(request);
            log.info("Milvus 创建集合 '{}'，使用 HNSW 索引", collectionName);

        } catch (Exception e) {
            log.error("Milvus 初始化集合 '{}' 失败", collectionName, e);
            throw new RuntimeException("Milvus 初始化集合失败", e);
        }
    }

    /**
     * 插入单个向量（如果已存在则更新）
     */
    public void upsert(long nodeId, float[] embedding) {
        if (!enabled) {
            log.debug("Milvus 未启用，跳过 upsert 节点: {}", nodeId);
            return;
        }

        if (embedding.length != vectorDim) {
            throw new IllegalArgumentException(
                    String.format("Embedding dimension mismatch: expected %d, got %d", vectorDim, embedding.length)
            );
        }

        try {
            JsonObject row = new JsonObject();
            row.addProperty("id", nodeId);
            row.add("embedding", gson.toJsonTree(embedding));

            // 使用 upsert：如果存在则更新，不存在则插入
            UpsertReq upsertReq = UpsertReq.builder()
                    .collectionName(getCollectionName())
                    .data(Collections.singletonList(row))
                    .build();

            milvusClient.upsert(upsertReq);
            log.debug("Milvus upsert 节点向量: {}", nodeId);

        } catch (Exception e) {
            log.error("Milvus upsert 节点向量失败: {}", nodeId, e);
            throw new RuntimeException("Milvus upsert 失败", e);
        }
    }

    /**
     * 批量插入向量（如果已存在则更新）
     */
    public void upsertBatch(List<Long> nodeIds, List<float[]> embeddings) {
        if (!enabled) {
            log.debug("Milvus 未启用，跳过批量 upsert {} 个节点", nodeIds.size());
            return;
        }

        if (nodeIds.size() != embeddings.size()) {
            throw new IllegalArgumentException("NodeIds and embeddings size mismatch");
        }

        if (nodeIds.isEmpty()) {
            return;
        }

        try {
            List<JsonObject> rows = new ArrayList<>();
            for (int i = 0; i < nodeIds.size(); i++) {
                JsonObject row = new JsonObject();
                row.addProperty("id", nodeIds.get(i));
                row.add("embedding", gson.toJsonTree(embeddings.get(i)));
                rows.add(row);
            }

            // 使用 upsert：如果存在则更新，不存在则插入
            UpsertReq upsertReq = UpsertReq.builder()
                    .collectionName(getCollectionName())
                    .data(rows)
                    .build();

            milvusClient.upsert(upsertReq);
            log.info("Milvus 批量 upsert {} 个向量", nodeIds.size());

        } catch (Exception e) {
            log.error("Milvus 批量 upsert {} 个向量失败", nodeIds.size(), e);
            throw new RuntimeException("Milvus 批量 upsert 失败", e);
        }
    }

    /**
     * 搜索结果
     */
    public record SearchResult(long nodeId, float score) {}

    /**
     * 搜索相似向量
     */
    public List<SearchResult> searchSimilar(float[] queryEmbedding, int topK, double threshold) {
        if (!enabled) {
            log.debug("Milvus 未启用，返回空搜索结果");
            return Collections.emptyList();
        }

        if (queryEmbedding.length != vectorDim) {
            throw new IllegalArgumentException("Query embedding dimension mismatch");
        }

        try {
            // 使用 FloatVec 包装向量
            FloatVec queryVector = new FloatVec(queryEmbedding);

            // 构建搜索请求
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(getCollectionName())
                    .data(Collections.singletonList(queryVector))
                    .annsField("embedding")
                    .topK(topK)
                    .outputFields(Collections.singletonList("id"))
                    .consistencyLevel(ConsistencyLevel.STRONG)
                    .build();

            // 执行搜索
            SearchResp searchResp = milvusClient.search(searchReq);
            List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();

            // 处理结果
            List<SearchResult> results = new ArrayList<>();
            if (!searchResults.isEmpty() && !searchResults.get(0).isEmpty()) {
                for (SearchResp.SearchResult result : searchResults.get(0)) {
                    float score = result.getScore();

                    // 余弦相似度：只返回大于阈值的结果
                    if (score >= threshold) {
                        Object idObj = result.getId();
                        long nodeId;
                        if (idObj instanceof Long) {
                            nodeId = (Long) idObj;
                        } else if (idObj instanceof Integer) {
                            nodeId = ((Integer) idObj).longValue();
                        } else {
                            nodeId = Long.parseLong(idObj.toString());
                        }
                        results.add(new SearchResult(nodeId, score));
                    }
                }
            }

            log.debug("Milvus 搜索到 {} 个相似节点（阈值={}）", results.size(), threshold);
            return results;

        } catch (Exception e) {
            log.error("Milvus 搜索相似节点失败", e);
            throw new RuntimeException("Milvus 搜索失败", e);
        }
    }

    /**
     * 更新向量（使用 upsert，自动覆盖）
     */
    public void update(long nodeId, float[] embedding) {
        upsert(nodeId, embedding);
        log.debug("Milvus 更新节点向量: {}", nodeId);
    }

    /**
     * 删除单个向量
     */
    public void delete(long nodeId) {
        if (!enabled) {
            log.debug("Milvus 未启用，跳过删除节点: {}", nodeId);
            return;
        }

        try {
            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(getCollectionName())
                    .ids(Collections.singletonList((Object) nodeId))
                    .build();

            milvusClient.delete(deleteReq);
            log.debug("Milvus 删除节点向量: {}", nodeId);

        } catch (Exception e) {
            log.error("Milvus 删除节点向量失败: {}", nodeId, e);
            // 删除失败不抛异常，避免影响主流程
        }
    }

    /**
     * 批量删除向量
     */
    public void deleteBatch(List<Long> nodeIds) {
        if (!enabled) {
            log.debug("Milvus 未启用，跳过批量删除 {} 个节点", nodeIds != null ? nodeIds.size() : 0);
            return;
        }

        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }

        try {
            // 转换为 List<Object>
            List<Object> ids = new ArrayList<>(nodeIds);

            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(getCollectionName())
                    .ids(ids)
                    .build();

            milvusClient.delete(deleteReq);
            log.info("Milvus 批量删除 {} 个向量", nodeIds.size());

        } catch (Exception e) {
            log.error("Milvus 批量删除 {} 个向量失败", nodeIds.size(), e);
        }
    }
}
