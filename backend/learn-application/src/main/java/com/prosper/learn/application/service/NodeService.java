package com.prosper.learn.application.service;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.dto.request.CreateNodeRequest;
import com.prosper.learn.application.dto.response.NodeDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.node.NodeDetailDTO;
import com.prosper.learn.application.dto.response.node.NodeSearchResultDTO;
import com.prosper.learn.application.dto.response.node.NodeWithCourseDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.node.NodeDomainService;
import com.prosper.learn.infrastructure.embedding.EmbeddingService;
import com.prosper.learn.infrastructure.embedding.MilvusService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 节点应用服务
 *
 * 负责协调跨子域逻辑、DTO转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeDomainService domainService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final NodeConverter nodeConverter;
    private final CourseConverter courseConverter;
    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;
    private final ContentStatsDomainService contentStatsDomainService;

    // ========== Query 方法（读操作）==========

    public NodeDTO getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid node ID");
        }
        NodeDO nodeDO = nodeDataService.getById(id);
        //return toWithCourseDTO(nodeDO);
        return toDTO(nodeDO);
    }

    /**
     * 批量加载节点信息
     */
    public Map<Long, NodeWithCourseDTO> getNodeMap(List<Long> ids) {
        if (ids.isEmpty()) return new HashMap<>();

        List<NodeDO> nodeList = nodeDataService.getByIds(ids);
        return toWithCourseDTO(nodeList).stream().collect(
                Collectors.toMap(NodeWithCourseDTO::getId, node -> node));
    }

    /**
     * 管理后台：按条件筛选节点列表
     */
    public List<NodeDetailDTO> listByFilter(ContentState state, Long nodeId, Long courseId, Long creatorId, Long lastId) {
        // 调用 DomainService 查询
        List<NodeDO> nodeDOList = domainService.listByFilter(nodeId, courseId, creatorId, state, lastId);

        // 管理后台使用 toDetailDTOInternal，返回原始数据，不做屏蔽处理
        return nodeDOList.stream()
                .map(nodeConverter::toDetailDTOInternal)
                .toList();
    }

    /**
     * 根据文本搜索相似节点
     *
     * @param query 查询文本（节点名称或描述）
     * @param topK 返回结果数量
     * @param threshold 相似度阈值（0-1之间，余弦相似度）
     * @return 相似节点列表（按相似度从高到低排序，包含相似度分数）
     */
    public List<NodeSearchResultDTO> searchSimilarNodes(String query, int topK, double threshold) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            // 生成查询文本的embedding
            float[] queryEmbedding = embeddingService.generateEmbedding(query.trim());

            // 在Milvus中搜索相似节点
            List<MilvusService.SearchResult> searchResults = milvusService.searchSimilar(queryEmbedding, topK, threshold);

            if (searchResults.isEmpty()) {
                return List.of();
            }

            // 提取节点ID列表
            List<Long> similarNodeIds = searchResults.stream()
                    .map(MilvusService.SearchResult::nodeId)
                    .toList();

            // 批量查询节点信息
            List<NodeDO> nodes = nodeDataService.getByIds(similarNodeIds);

            // 转换为DTO并保持顺序（按相似度排序），同时设置相似度分数
            Map<Long, NodeDO> nodeMap = nodes.stream()
                    .collect(Collectors.toMap(NodeDO::getId, node -> node));

            Map<Long, Float> scoreMap = searchResults.stream()
                    .collect(Collectors.toMap(MilvusService.SearchResult::nodeId, MilvusService.SearchResult::score));

            return similarNodeIds.stream()
                    .map(nodeMap::get)
                    .filter(node -> node != null && node.getState() == ContentState.PUBLISHED.value())
                    .map(node -> toNodeSearchResultDTO(node, scoreMap.get(node.getId())))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to search similar nodes for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * 检查课程内是否存在同名已发布节点
     *
     * @param courseId 课程ID
     * @param name 节点名称
     * @return true表示存在重名节点，false表示不存在
     */
    public boolean checkDuplicateNode(Long courseId, String name) {
        if (courseId == null || name == null || name.trim().isEmpty()) {
            return false;
        }

        NodeDO node = nodeDataService.getByCourseAndName(courseId, name);

        // 只检查已发布状态的节点
        return node != null && node.getState() == ContentState.PUBLISHED.value();
    }

    // ========== DTO 转换方法 ==========

    public NodeDTO toDTO(NodeDO nodeDO) {
        if (nodeDO == null) return null;
        return nodeConverter.toDTO(nodeDO);

    }

    public NodeWithCourseDTO toWithCourseDTO(NodeDO nodeDO) {
        if (nodeDO == null)  return null;

        NodeWithCourseDTO dto = nodeConverter.toWithCourseDTO(nodeDO);
        if (dto != null && nodeDO.getCourseId() != null) {
            CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
            dto.setCourse(courseConverter.toSummaryDTO(courseDO));
        }

        // 填充统计数据（commentCount 和 nodeReferenceCount）
        if (dto != null) {
            ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.node, nodeDO.getId());
            if (stats != null) {
                dto.setCommentCount(stats.getCommentCount() != null ? stats.getCommentCount() : 0);
                dto.setNodeReferenceCount(stats.getNodeReferenceCount() != null ? stats.getNodeReferenceCount() : 0);
            }
        }

        return dto;
    }

    public List<NodeWithCourseDTO> toWithCourseDTO(List<NodeDO> nodeDOList) {
        if (nodeDOList == null || nodeDOList.isEmpty()) return List.of();

        List<NodeWithCourseDTO> dtoList = nodeConverter.toWithCourseDTO(nodeDOList);

        // 批量加载课程信息（基于节点）
        List<Long> courseIds = Utils.getIds(nodeDOList, dto -> ((NodeDO) dto).getCourseId());
        Map<Long, CourseDO> courseMap = courseDataService.getMapByIds(courseIds);

        // 批量加载节点引用计数
        List<Long> nodeIds = nodeDOList.stream().map(NodeDO::getId).toList();
        Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(ContentType.node, nodeIds);

        for (NodeWithCourseDTO dto : dtoList) {
            dto.setCourse(courseConverter.toSummaryDTO(courseMap.get(dto.getCourseId())));

            // 填充统计数据（commentCount 和 nodeReferenceCount）
            ContentStatsDTO stats = statsMap.get(dto.getId());
            dto.setCommentCount(stats != null && stats.getCommentCount() != null ? stats.getCommentCount() : 0);
            dto.setNodeReferenceCount(stats != null && stats.getNodeReferenceCount() != null ? stats.getNodeReferenceCount() : 0);
        }
        return dtoList;
    }

    /**
     * 转换为带相似度的搜索结果DTO
     */
    private NodeSearchResultDTO toNodeSearchResultDTO(NodeDO nodeDO, Float similarityScore) {
        NodeWithCourseDTO baseDto = toWithCourseDTO(nodeDO);
        if (baseDto == null) {
            return null;
        }

        NodeSearchResultDTO dto = new NodeSearchResultDTO();
        dto.setId(baseDto.getId());
        dto.setName(baseDto.getName());
        dto.setDescription(baseDto.getDescription());
        dto.setCourseId(baseDto.getCourseId());
        dto.setCreatorId(baseDto.getCreatorId());
        dto.setState(baseDto.getState());
        dto.setCommentCount(baseDto.getCommentCount());
        dto.setNodeReferenceCount(baseDto.getNodeReferenceCount());
        dto.setCreatedAt(baseDto.getCreatedAt());
        dto.setUpdatedAt(baseDto.getUpdatedAt());
        dto.setCourse(baseDto.getCourse());
        dto.setSimilarityScore(similarityScore);

        return dto;
    }

    // ========== Command 方法（写操作）==========

    /**
     * 修改节点状态
     */
    @Transactional
    public NodeDetailDTO updateNodeState(Long nodeId, ContentState state, String reason) {
        // 调用 DomainService 执行状态变更
        domainService.updateNodeState(nodeId, state, reason);

        // 查询并返回 DTO
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        return nodeConverter.toDetailDTO(nodeDO);
    }

    /**
     * 审批通过节点
     */
    @Transactional
    public void approve(Long nodeId) {
        domainService.approve(nodeId);
    }

    /**
     * 拒绝节点（审核不通过）
     */
    @Transactional
    public void reject(Long nodeId, String reason) {
        domainService.reject(nodeId, reason);
    }

    /**
     * 封禁节点（违规封禁）
     */
    @Transactional
    public void ban(Long nodeId, String reason) {
        domainService.ban(nodeId, reason);
    }

    /**
     * 批量初始化现有节点的embedding
     * 用于系统初始化或补偿机制
     *
     * @param batchSize 每批处理数量
     * @return 处理结果统计
     */
    public Map<String, Object> initializeNodeEmbeddings(int batchSize) {
        int successCount = 0;
        int failCount = 0;
        long lastId = 0;

        try {
            while (true) {
                // 分批查询已发布的节点
                List<NodeDO> nodes = nodeDataService.getListByFilter(null, null, null,
                        ContentState.PUBLISHED.value(), lastId, batchSize);

                if (nodes.isEmpty()) {
                    break;
                }

                log.info("Processing batch: {} nodes, starting from ID: {}", nodes.size(), lastId);

                try {
                    // 准备批量文本
                    List<String> texts = nodes.stream()
                            .map(node -> node.getName() + "\n\n" + node.getDescription())
                            .toList();

                    // 批量生成 embedding
                    List<float[]> embeddings = embeddingService.generateEmbeddingsBatch(texts);

                    // 批量插入 Milvus
                    List<Long> nodeIds = nodes.stream().map(NodeDO::getId).toList();
                    milvusService.upsertBatch(nodeIds, embeddings);

                    successCount += nodes.size();
                    log.info("Batch upsert successful: {} nodes", nodes.size());

                } catch (Exception e) {
                    // 批量失败时，回退到逐个处理
                    log.warn("Batch processing failed, falling back to individual processing", e);
                    for (NodeDO node : nodes) {
                        try {
                            String text = node.getName() + "\n\n" + node.getDescription();
                            float[] embedding = embeddingService.generateEmbedding(text);
                            milvusService.upsert(node.getId(), embedding);
                            successCount++;
                        } catch (Exception ex) {
                            failCount++;
                            log.error("Failed to initialize embedding for node: {} ({})", node.getId(), node.getName(), ex);
                        }
                    }
                }

                // 更新游标
                lastId = nodes.get(nodes.size() - 1).getId();

                // 如果本批数量小于限制，说明已经是最后一批
                if (nodes.size() < batchSize) {
                    break;
                }

                log.info("Batch completed: success={}, fail={}, nextId={}", successCount, failCount, lastId);
            }
        } catch (Exception e) {
            log.error("Batch initialization failed", e);
        }

        log.info("Embedding initialization completed: total={}, success={}, fail={}",
                successCount + failCount, successCount, failCount);

        return Map.of(
                "successCount", successCount,
                "failCount", failCount,
                "totalProcessed", successCount + failCount
        );
    }

    /**
     * 创建节点并自动审核通过（Admin专用）
     */
    @Transactional
    public Long createAndApprove(CreateNodeRequest request, UserDO creator) {
        // 验证参数
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("节点创建请求不能为空");
        }

        // 验证课程存在
        CourseDO course = courseDataService.getById(request.getCourseId());
        if (course == null) {
            throw StatusCode.INVALID_PARAMETER.exception("课程不存在");
        }

        // 检查同一课程下是否已存在同名节点
        if (checkDuplicateNode(request.getCourseId(), request.getName())) {
            throw StatusCode.INVALID_PARAMETER.exception(
                "课程《" + course.getName() + "》下已存在名为《" + request.getName() + "》的节点"
            );
        }

        // 创建节点（状态为SUBMITTED）
        NodeDO node = new NodeDO(
                creator.getId(),
                request.getCourseId(),
                request.getName(),
                request.getDescription(),
                ContentState.SUBMITTED.value(), // 先设置为待审核
                Bool.FALSE.value() // isCourseRoot = false
        );
        nodeDataService.insert(node);

        // 审核通过
        domainService.approve(node.getId());

        return node.getId();
    }
}
