package com.prosper.learn.content.roadmap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.shared.common.utils.UnionFind;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.prosper.learn.shared.domain.Enums.ContentState;

/**
 * 路线图领域服务
 *
 * 只依赖 content 域，处理路线图的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapDomainService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RoadmapDataService roadmapDataService;

    // ========== Query 方法 ==========

    /**
     * 按状态查询路线图列表
     */
    public List<RoadmapDO> listByState(ContentState state, Long lastId, int limit) {
        return roadmapDataService.listByState(state != null ? state.value() : null, lastId, limit);
    }

    /**
     * 高级筛选路线图列表
     */
    public List<RoadmapDO> listByFilter(Long roadmapId, Long professionId, Long creatorId, Long lastId, int limit) {
        return roadmapDataService.listByFilter(roadmapId, professionId, creatorId, lastId, limit);
    }

    /**
     * 获取职业路线图列表（公开接口）
     * 用于匿名用户浏览，按分数排序
     *
     * @param professionId 职业ID
     * @param lastId 最后一个路线图ID（分页游标）
     * @param limit 查询数量限制
     * @return 路线图列表
     */
    public List<RoadmapDO> getRoadmapsByProfessionPublic(Long professionId, Long lastId, int limit) {
        if (lastId == null || lastId == 0) {
            return roadmapDataService.getListByProfessionOrderBy(professionId, limit, "score");
        } else {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                return roadmapDataService.getListByProfessionAfterCursorOrderBy(
                    professionId, lastRoadmap.getScore(), lastRoadmap.getCreatedAt(), lastId, limit, "score");
            } else {
                return new ArrayList<>();
            }
        }
    }

    /**
     * 获取职业路线图列表（支持动态排序）
     */
    public List<RoadmapDO> getRoadmapsByProfession(Long professionId, Long lastId, int limit, String sortBy) {
        if (lastId != null) {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                return roadmapDataService.getListByProfessionAfterCursorOrderBy(
                    professionId, lastRoadmap.getScore(), lastRoadmap.getCreatedAt(), lastId, limit, sortBy);
            }
        }

        // 首次加载
        return roadmapDataService.getListByProfessionOrderBy(professionId, limit, sortBy);
    }

    /**
     * 获取用户创建的路线图列表
     *
     * @param userId 用户ID
     * @param lastId 分页游标
     * @param limit 查询数量限制
     * @param state 状态过滤（可选）
     * @return 路线图列表
     */
    public List<RoadmapDO> getUserRoadmaps(Long userId, Long lastId, int limit, Byte state) {
        return roadmapDataService.getListByCreatorWithPaging(userId, lastId, limit, state);
    }

    /**
     * 根据ID列表批量获取路线图
     * @param roadmapIds 路线图ID列表
     * @return 路线图列表
     */
    public List<RoadmapDO> getRoadmapsByIds(List<Long> roadmapIds) {
        return roadmapDataService.getByIds(roadmapIds);
    }

    /**
     * 标准化content内容并计算hash值
     * @param content 原始content字符串，格式: [[[1,2],[2,3]],[1,2,3]]
     * @return 标准化后的hash值
     */
    public String calculateContentHash(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                throw StatusCode.ROADMAP_CONTENT_INVALID.exception();
            }

            JsonNode edgesNode = rootNode.get(0);
            JsonNode nodesNode = rootNode.get(1);

            List<List<Integer>> edges = new ArrayList<>();
            for (JsonNode edge : edgesNode) {
                List<Integer> edgePair = new ArrayList<>();
                edgePair.add(edge.get(0).asInt());
                edgePair.add(edge.get(1).asInt());
                edges.add(edgePair);
            }

            edges.sort((a, b) -> {
                int firstCompare = Integer.compare(a.get(0), b.get(0));
                return firstCompare != 0 ? firstCompare : Integer.compare(a.get(1), b.get(1));
            });

            List<Integer> nodes = new ArrayList<>();
            for (JsonNode node : nodesNode) {
                nodes.add(node.asInt());
            }
            Collections.sort(nodes);

            ArrayNode standardizedContent = JsonNodeFactory.instance.arrayNode();

            ArrayNode standardizedEdges = JsonNodeFactory.instance.arrayNode();
            for (List<Integer> edge : edges) {
                ArrayNode edgeArray = JsonNodeFactory.instance.arrayNode();
                edgeArray.add(edge.get(0));
                edgeArray.add(edge.get(1));
                standardizedEdges.add(edgeArray);
            }
            standardizedContent.add(standardizedEdges);

            ArrayNode standardizedNodes = JsonNodeFactory.instance.arrayNode();
            for (Integer node : nodes) {
                standardizedNodes.add(node);
            }
            standardizedContent.add(standardizedNodes);

            String standardizedString = standardizedContent.toString();
            return Utils.md5(standardizedString);

        } catch (Exception e) {
            log.error("内容哈希计算失败: {}", content, e);
            throw StatusCode.CONTENT_HASH_ERROR.exception(e);
        }
    }

    /**
     * 验证content基本格式和边的节点存在性（不验证树结构）
     * 用于草稿模式，允许有孤立节点，但边必须指向存在的节点
     */
    public boolean isValidContentBasicFormat(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                return false;
            }

            JsonNode edgesNode = rootNode.get(0);
            JsonNode nodesNode = rootNode.get(1);

            // 验证节点数组格式
            if (!nodesNode.isArray()) {
                return false;
            }
            Set<Integer> nodeSet = new HashSet<>();
            for (JsonNode node : nodesNode) {
                if (!node.isInt()) {
                    return false;
                }
                nodeSet.add(node.asInt());
            }

            // 验证边数组格式和节点存在性
            if (!edgesNode.isArray()) {
                return false;
            }
            for (JsonNode edge : edgesNode) {
                if (!edge.isArray() || edge.size() != 2) {
                    return false;
                }
                if (!edge.get(0).isInt() || !edge.get(1).isInt()) {
                    return false;
                }
                // 验证边的两个节点都在节点集合中
                int source = edge.get(0).asInt();
                int target = edge.get(1).asInt();
                if (!nodeSet.contains(source) || !nodeSet.contains(target)) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.warn("内容基本格式验证失败", e);
            return false;
        }
    }

    /**
     * 验证content格式是否正确并且是一棵树
     */
    public boolean isValidContentFormat(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                return false;
            }

            JsonNode edgesNode = rootNode.get(0);
            JsonNode nodesNode = rootNode.get(1);

            if (!edgesNode.isArray()) {
                return false;
            }

            List<int[]> edges = new ArrayList<>();
            for (JsonNode edge : edgesNode) {
                if (!edge.isArray() || edge.size() != 2) {
                    return false;
                }
                if (!edge.get(0).isInt() || !edge.get(1).isInt()) {
                    return false;
                }
                edges.add(new int[]{edge.get(0).asInt(), edge.get(1).asInt()});
            }

            if (!nodesNode.isArray()) {
                return false;
            }

            Set<Integer> nodeSet = new HashSet<>();
            for (JsonNode node : nodesNode) {
                if (!node.isInt()) {
                    return false;
                }
                nodeSet.add(node.asInt());
            }

            return isValidTree(edges, nodeSet);

        } catch (Exception e) {
            log.warn("内容格式验证失败", e);
            return false;
        }
    }

    /**
     * 校验是否是有效的树结构
     * @param edges 边的列表
     * @param nodes 节点集合
     * @return 是否是有效的树
     */
    private boolean isValidTree(List<int[]> edges, Set<Integer> nodes) {
        int nodeCount = nodes.size();
        int edgeCount = edges.size();

        // 特殊情况：只有一个节点且没有边，也是树
        if (nodeCount == 1 && edgeCount == 0) {
            return true;
        }

        // 树的必要条件：n个节点，n-1条边
        if (edgeCount != nodeCount - 1) {
            return false;
        }

        // 检查所有边的节点是否都在节点集合中
        for (int[] edge : edges) {
            if (!nodes.contains(edge[0]) || !nodes.contains(edge[1])) {
                return false;
            }
        }

        // 使用并查集检查连通性和是否有环
        UnionFind uf = new UnionFind(nodes);

        for (int[] edge : edges) {
            int u = edge[0];
            int v = edge[1];

            // 如果两个节点已经连通，说明有环
            if (uf.connected(u, v)) {
                return false;
            }

            uf.union(u, v);
        }

        // 检查是否所有节点都连通
        return uf.getComponentCount() == 1;
    }

    // ========== Command 方法 ==========

    /**
     * 创建路线图
     */
    @Transactional
    public Long createRoadmap(long professionId, String content, String description, long userId, int nodeCount, Byte state) {
        // Domain 层安全验证：状态只能是草稿或提交审核
        if (!ContentState.DRAFT.value().equals(state) && !ContentState.SUBMITTED.value().equals(state)) {
            throw new IllegalArgumentException("状态非法");
        }

        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setDescription(description);
        roadmapDO.setProfessionId(professionId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setNodeCount(nodeCount);
        roadmapDO.setState(state);
        roadmapDO.setScore(0.0);

        roadmapDataService.insert(roadmapDO);
        log.info("路线图 创建成功: roadmapId={}，professionId={}，userId={}，state={}",
            roadmapDO.getId(), professionId, userId, state);

        return roadmapDO.getId();
    }

    /**
     * 更新路线图
     */
    @Transactional
    public void updateRoadmap(long id, String content, int nodeCount) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setNodeCount(nodeCount);
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapDataService.update(roadmapDO);
        log.info("路线图 更新成功: roadmapId={}", id);
    }

    /**
     * 删除路线图（软删除）
     */
    @Transactional
    public void deleteRoadmap(long id) {
        roadmapDataService.validateExists(id);

        int result = roadmapDataService.softDelete(id);
        if (result == 0) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        log.info("路线图 删除成功: roadmapId={}", id);
    }

    /**
     * 批准路线图
     */
    @Transactional
    public void approve(long id) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.PUBLISHED);

        roadmapDataService.approve(id);
        log.info("路线图 审核通过: roadmapId={}", id);
    }

    /**
     * 拒绝路线图
     */
    @Transactional
    public void reject(long id, String reason) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.REJECTED);

        roadmapDataService.reject(id, reason);
        log.info("路线图 审核拒绝: roadmapId={}，reason={}", id, reason);
    }

    /**
     * 封禁路线图
     */
    @Transactional
    public void ban(long id, String reason) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.BANNED);

        roadmapDataService.ban(id, reason);
        log.info("路线图 封禁: roadmapId={}，reason={}", id, reason);
    }

    /**
     * 更新路线图描述（管理员操作）
     */
    @Transactional
    public void updateDescription(long id, String description) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        roadmap.setDescription(description != null ? description : "");
        roadmapDataService.update(roadmap);

        log.info("路线图 描述更新成功: roadmapId={}", id);
    }

    // ========== 内容解析方法 ==========

    /**
     * 解析路线图内容为图形格式
     *
     * @param content 原始内容
     * @param nodeProgress 节点ID到进度百分比的映射（0-10000）
     * @param nodeMap 节点ID到节点实体的映射（包含 name, is_course_root, course_id 等）
     * @return JSON格式的图形数据
     */
    public String parseContentToGraphFormat(String content,
                                           Map<Long, Integer> nodeProgress,
                                           Map<Long, NodeDO> nodeMap) {
        try {
            List<List<Object>> contentData = objectMapper.readValue(content, new TypeReference<>() {});

            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, String>> edges = new ArrayList<>();
            List<Map<String, Object>> nodes = new ArrayList<>();

            if (contentData.size() >= 2) {
                edges = parseEdges(contentData.get(0));
                nodes = parseNodes(contentData.get(1), nodeProgress, nodeMap);
            }

            graphData.put("edges", edges);
            graphData.put("nodes", nodes);

            return objectMapper.writeValueAsString(graphData);
        } catch (Exception e) {
            log.error("内容解析为图形格式失败", e);
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    /**
     * 解析边数据
     */
    private List<Map<String, String>> parseEdges(List<Object> edgeDataRaw) {
        List<Map<String, String>> edges = new ArrayList<>();
        for (Object edgeObj : edgeDataRaw) {
            if (edgeObj instanceof List) {
                List<Object> edge = (List<Object>) edgeObj;
                if (edge.size() >= 2) {
                    Map<String, String> edgeMap = new HashMap<>();
                    edgeMap.put("source", String.valueOf(edge.get(0)));
                    edgeMap.put("target", String.valueOf(edge.get(1)));
                    edges.add(edgeMap);
                }
            }
        }
        return edges;
    }

    /**
     * 解析节点数据
     *
     * @param nodeIdsRaw 原始节点ID列表
     * @param nodeProgress 节点进度映射（0-10000）
     * @param nodeDataMap 节点实体映射（包含所有元数据）
     * @return 节点列表
     */
    private List<Map<String, Object>> parseNodes(List<Object> nodeIdsRaw,
                                                 Map<Long, Integer> nodeProgress,
                                                 Map<Long, NodeDO> nodeDataMap) {
        List<Long> nodeIds = new ArrayList<>();
        for (Object nodeIdObj : nodeIdsRaw) {
            if (nodeIdObj instanceof Number) {
                nodeIds.add(((Number) nodeIdObj).longValue());
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (long nodeId : nodeIds) {
            // 获取节点实体
            NodeDO nodeDO = nodeDataMap.get(nodeId);
            String nodeName = nodeDO != null && nodeDO.getName() != null ? nodeDO.getName() : "节点" + nodeId;

            // 获取进度信息（0-10000，转换为 0.0-100.0）
            Integer progressInt = nodeProgress.get(nodeId);
            double progressPercent = progressInt != null ? progressInt / 100.0 : 0.0;

            // 获取节点元数据
            boolean isCourseRoot = nodeDO != null && nodeDO.getIsCourseRoot() != null && nodeDO.getIsCourseRoot() == 1;
            Long courseId = nodeDO != null ? nodeDO.getCourseId() : null;

            // 构建节点数据
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", String.valueOf(nodeId));
            nodeMap.put("name", nodeName);
            nodeMap.put("progress", progressPercent);
            nodeMap.put("isCourseRoot", isCourseRoot);
            if (courseId != null) {
                nodeMap.put("courseId", courseId);
            }
            nodes.add(nodeMap);
        }

        return nodes;
    }

}
