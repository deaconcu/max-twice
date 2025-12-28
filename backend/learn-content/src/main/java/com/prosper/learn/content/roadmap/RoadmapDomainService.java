package com.prosper.learn.content.roadmap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
     * 按条件筛选路线图列表
     */
    public List<RoadmapDO> listByFilter(Byte state, Long professionId, Long creatorId, Long lastId) {
        return roadmapDataService.listByFilter(state, professionId, creatorId, lastId);
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
            return roadmapDataService.getListByProfessionExcludingOrderByScore(
                professionId, limit, new ArrayList<>());
        } else {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                return roadmapDataService.getListByProfessionAfterScoreExcluding(
                    professionId, lastRoadmap.getScore(), lastId, limit, null);
            } else {
                return new ArrayList<>();
            }
        }
    }

    /**
     * 获取职业路线图列表（带置顶）
     * 支持首页置顶和分页查询
     *
     * @param professionId 职业ID
     * @param lastId 最后一个路线图ID（分页游标，null表示首页）
     * @param pinnedRoadmapIds 置顶的路线图ID列表（由外部提供）
     * @param limit 查询数量限制
     * @return 路线图列表
     */
    public List<RoadmapDO> getRoadmapsByProfessionWithPinned(Long professionId, Long lastId,
                                                              List<Long> pinnedRoadmapIds, int limit) {
        List<RoadmapDO> roadmapList = new ArrayList<>();

        // 首页：先加载置顶路线图
        if (lastId == null && pinnedRoadmapIds != null && !pinnedRoadmapIds.isEmpty()) {
            List<RoadmapDO> pinnedRoadmaps = roadmapDataService.getByIds(pinnedRoadmapIds);
            roadmapList.addAll(pinnedRoadmaps);

            // 加载其他路线图补充到 limit
            int remainingLimit = limit - roadmapList.size();
            if (remainingLimit > 0) {
                List<RoadmapDO> otherRoadmaps = roadmapDataService.getListByProfessionExcludingOrderByScore(
                    professionId, remainingLimit, pinnedRoadmapIds);
                roadmapList.addAll(otherRoadmaps);
            }
        }
        // 分页：按分数查询
        else {
            if (lastId != null) {
                RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
                if (lastRoadmap != null) {
                    roadmapList = roadmapDataService.getListByProfessionAfterScoreExcluding(
                        professionId, lastRoadmap.getScore(), lastId, limit, null);
                }
            } else {
                // lastId 为 null 但没有置顶，直接查询
                roadmapList = roadmapDataService.getListByProfessionExcludingOrderByScore(
                    professionId, limit, new ArrayList<>());
            }
        }

        return roadmapList;
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
    public Long createRoadmap(Long professionId, String content, String description, Long userId, Integer nodeCount) {
        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setDescription(description);
        roadmapDO.setProfessionId(professionId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setNodeCount(nodeCount);

        roadmapDataService.insert(roadmapDO);
        log.info("Created roadmap: {} for profession: {} by user: {}", roadmapDO.getId(), professionId, userId);

        return roadmapDO.getId();
    }

    /**
     * 更新路线图
     */
    @Transactional
    public void updateRoadmap(Long id, String content, Integer nodeCount) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setNodeCount(nodeCount);
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapDataService.update(roadmapDO);
        log.info("Updated roadmap: {}", id);
    }

    /**
     * 删除路线图（软删除）
     */
    @Transactional
    public void deleteRoadmap(Long id) {
        roadmapDataService.validateExists(id);

        int result = roadmapDataService.softDelete(id);
        if (result == 0) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
        log.info("Deleted roadmap: {}", id);
    }

    /**
     * 批准路线图
     */
    @Transactional
    public void approve(Long id) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.PUBLISHED);

        roadmapDataService.approve(id);
        log.info("Roadmap {} approved", id);
    }

    /**
     * 拒绝路线图
     */
    @Transactional
    public void reject(Long id, String reason) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.REJECTED);

        roadmapDataService.reject(id, reason);
        log.info("Roadmap {} rejected, reason: {}", id, reason);
    }

    /**
     * 封禁路线图
     */
    @Transactional
    public void ban(Long id, String reason) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        Utils.validateStateTransition(roadmap.getState(), ContentState.BANNED);

        roadmapDataService.ban(id, reason);
        log.info("Roadmap {} banned, reason: {}", id, reason);
    }

    /**
     * 更新路线图描述（管理员操作）
     */
    @Transactional
    public void updateDescription(Long id, String description) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        roadmap.setDescription(description != null ? description : "");
        roadmapDataService.update(roadmap);

        log.info("Updated description for roadmap: {}", id);
    }

    /**
     * 清除描述并批准路线图
     */
    @Transactional
    public void approveAndClearDescription(Long id) {
        roadmapDataService.validateExists(id);

        RoadmapDO roadmap = roadmapDataService.getById(id);
        roadmap.setDescription("");
        roadmap.setState(ContentState.PUBLISHED.value());
        roadmapDataService.update(roadmap);

        log.info("Approved and cleared description for roadmap: {}", id);
    }

    // ========== 内容解析方法 ==========

    /**
     * 解析路线图内容为图形格式
     *
     * @param content 原始内容
     * @param courseNames 课程ID到名称的映射
     * @param courseProgress 课程ID到进度信息的映射（完成状态、进度百分比）
     * @return JSON格式的图形数据
     */
    public String parseContentToGraphFormat(String content,
                                           Map<Long, String> courseNames,
                                           Map<Long, CourseProgress> courseProgress) {
        try {
            List<List<Object>> contentData = objectMapper.readValue(content, new TypeReference<>() {});

            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, String>> edges = new ArrayList<>();
            List<Map<String, Object>> nodes = new ArrayList<>();

            if (contentData.size() >= 2) {
                edges = parseEdges(contentData.get(0));
                nodes = parseNodes(contentData.get(1), courseNames, courseProgress);
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
     * @param courseNames 课程名称映射
     * @param courseProgress 课程进度映射
     * @return 节点列表
     */
    private List<Map<String, Object>> parseNodes(List<Object> nodeIdsRaw,
                                                 Map<Long, String> courseNames,
                                                 Map<Long, CourseProgress> courseProgress) {
        List<Long> nodeIds = new ArrayList<>();
        for (Object nodeIdObj : nodeIdsRaw) {
            if (nodeIdObj instanceof Number) {
                nodeIds.add(((Number) nodeIdObj).longValue());
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (long nodeId : nodeIds) {
            String courseName = courseNames.getOrDefault(nodeId, "课程" + nodeId);

            CourseProgress progress = courseProgress.get(nodeId);
            boolean finished = progress != null && progress.isFinished();
            double progressPercent = progress != null ? progress.getProgressPercent() : 0.0;

            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", String.valueOf(nodeId));
            nodeMap.put("name", courseName);
            nodeMap.put("finished", finished);
            nodeMap.put("progress", progressPercent);
            nodes.add(nodeMap);
        }

        return nodes;
    }

    /**
     * 课程进度信息（内部使用）
     */
    public static class CourseProgress {
        private final boolean finished;
        private final double progressPercent;

        public CourseProgress(boolean finished, double progressPercent) {
            this.finished = finished;
            this.progressPercent = progressPercent;
        }

        public boolean isFinished() {
            return finished;
        }

        public double getProgressPercent() {
            return progressPercent;
        }
    }
}
