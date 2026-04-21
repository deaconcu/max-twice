package com.twicemax.application.service;

import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import com.twicemax.analytics.stats.service.RedisStatsDomainService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.post.PostDataService;
import com.twicemax.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 统计应用服务
 *
 * 负责统计相关的业务逻辑和验证：
 * - 验证业务对象是否存在
 * - 协调领域服务完成统计记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final PostDataService postDataService;
    private final RedisStatsDomainService redisStatsDomainService;
    private final ContentStatsDataService contentStatsDataService;

    /**
     * 记录文章访问
     *
     * @param postId 文章ID
     * @param userId 用户ID（0表示匿名访问）
     */
    public void recordPostView(long postId, long userId) {
        // 验证文章是否存在
        postDataService.validateAndGet(postId);

        // 记录访问统计
        redisStatsDomainService.recordArticleView(postId, userId);

        log.debug("记录文章访问: postId={}, userId={}", postId, userId);
    }

    /**
     * 重新计算所有节点的引用数统计
     *
     * @return 处理结果统计
     */
    @Transactional
    public Map<String, Object> recalculateNodeReferenceCount() {
        log.info("开始重新计算所有节点的引用数统计");

        int processedPostCount = 0;
        int updatedNodeCount = 0;
        long lastId = 0;
        int batchSize = 100;

        try {
            // 统计每个节点被引用的次数
            Map<Long, Integer> nodeRefCountMap = new HashMap<>();

            // 查询所有已发布的 index 类型 post
            while (true) {
                List<PostDO> indexPosts = postDataService.getPostsByTypeAndState(
                    PostType.index.value(),
                    ContentState.PUBLISHED.value(),
                    lastId,
                    batchSize
                );

                if (indexPosts.isEmpty()) {
                    break;
                }

                for (PostDO post : indexPosts) {
                    List<Long> referencedNodeIds = parseReferencedNodeIds(post.getContent());
                    for (Long nodeId : referencedNodeIds) {
                        nodeRefCountMap.merge(nodeId, 1, Integer::sum);
                    }
                }

                processedPostCount += indexPosts.size();
                lastId = indexPosts.get(indexPosts.size() - 1).getId();

                // 每处理1000个post输出一次进度
                if (processedPostCount % 1000 == 0) {
                    log.info("已处理 {} 个目录型帖子，当前统计到 {} 个被引用节点",
                        processedPostCount, nodeRefCountMap.size());
                }

                if (indexPosts.size() < batchSize) {
                    break;
                }
            }

            log.info("帖子处理完成，共 {} 个目录型帖子，涉及 {} 个被引用节点，开始更新统计数据...",
                processedPostCount, nodeRefCountMap.size());

            // 更新所有节点的引用数统计
            for (Map.Entry<Long, Integer> entry : nodeRefCountMap.entrySet()) {
                contentStatsDataService.setNodeReferenceCount(
                    ContentType.node,
                    entry.getKey(),
                    entry.getValue()
                );
                updatedNodeCount++;

                // 每更新1000个节点输出一次进度
                if (updatedNodeCount % 1000 == 0) {
                    log.info("已更新 {} 个节点的引用数统计", updatedNodeCount);
                }
            }

            log.info("节点引用数统计重新计算完成: processedPosts={}, updatedNodes={}",
                processedPostCount, updatedNodeCount);

            return Map.of(
                "processedPosts", processedPostCount,
                "updatedNodes", updatedNodeCount
            );

        } catch (Exception e) {
            log.error("重新计算节点引用数失败", e);
            throw e;
        }
    }

    /**
     * 解析 index 类型帖子的引用节点ID列表
     */
    private List<Long> parseReferencedNodeIds(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }

        try {
            return Arrays.stream(content.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            log.warn("统计服务 解析引用的节点ID失败: {}", content, e);
            return List.of();
        }
    }
}
