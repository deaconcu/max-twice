package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.prosper.learn.common.UnionFind;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CourseMapper courseMapper;
    private final RoadmapMapper roadmapMapper;
    private final UserMapper userMapper;
    private final UpvoteService upvoteService;
    private final UserCourseService userCourseService;

    public RoadmapDTO getById(int id, int userId) {
        RoadmapDO roadmapDO = roadmapMapper.get(id);
        if (roadmapDO == null) return null;

        RoadmapDTO dto = Converter.INSTANCE.toRoadmapDTOWithUser(roadmapDO, userMapper);
        dto.setUpvoted(upvoteService.hasUpvotedRoadmap(dto.getId(), userId));
        return dto;
    }
    /**
     * 标准化content内容并计算hash值
     * @param content 原始content字符串，格式: [[[1,2],[2,3]],[1,2,3]]
     * @return 标准化后的hash值
     */
    public static String calculateContentHash(String content) {
        try {
            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
            }

            // 获取edges和nodes
            JsonNode edgesNode = rootNode.get(0);
            JsonNode nodesNode = rootNode.get(1);

            // 标准化edges（排序）
            List<List<Integer>> edges = new ArrayList<>();
            for (JsonNode edge : edgesNode) {
                List<Integer> edgePair = new ArrayList<>();
                edgePair.add(edge.get(0).asInt());
                edgePair.add(edge.get(1).asInt());
                edges.add(edgePair);
            }
            // 对edges进行排序，先按第一个元素排序，再按第二个元素排序
            edges.sort((a, b) -> {
                int firstCompare = Integer.compare(a.get(0), b.get(0));
                return firstCompare != 0 ? firstCompare : Integer.compare(a.get(1), b.get(1));
            });

            // 标准化nodes（排序）
            List<Integer> nodes = new ArrayList<>();
            for (JsonNode node : nodesNode) {
                nodes.add(node.asInt());
            }
            Collections.sort(nodes);

            // 构建标准化的JSON
            ArrayNode standardizedContent = JsonNodeFactory.instance.arrayNode();
            
            // 添加标准化的edges
            ArrayNode standardizedEdges = JsonNodeFactory.instance.arrayNode();
            for (List<Integer> edge : edges) {
                ArrayNode edgeArray = JsonNodeFactory.instance.arrayNode();
                edgeArray.add(edge.get(0));
                edgeArray.add(edge.get(1));
                standardizedEdges.add(edgeArray);
            }
            standardizedContent.add(standardizedEdges);

            // 添加标准化的nodes
            ArrayNode standardizedNodes = JsonNodeFactory.instance.arrayNode();
            for (Integer node : nodes) {
                standardizedNodes.add(node);
            }
            standardizedContent.add(standardizedNodes);

            // 计算hash
            String standardizedString = standardizedContent.toString();
            return Utils.md5(standardizedString);

        } catch (Exception e) {
            throw ErrorCode.CONTENT_HASH_ERROR.exception(e);
        }
    }

    /**
     * 计算MD5哈希值
     */
    private static String calculateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw ErrorCode.CONTENT_HASH_ERROR.exception(e);
        }
    }

    /**
     * 验证content格式是否正确并且是一棵树
     */
    public static boolean isValidContentFormat(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                return false;
            }

            JsonNode edgesNode = rootNode.get(0);
            JsonNode nodesNode = rootNode.get(1);

            // 验证edges格式
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

            // 验证nodes格式
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

            // 校验是否是树结构
            return isValidTree(edges, nodeSet);

        } catch (Exception e) {
            log.warn("Failed to validate content format", e);
            return false;
        }
    }

    /**
     * 校验是否是有效的树结构
     * @param edges 边的列表
     * @param nodes 节点集合
     * @return 是否是有效的树
     */
    private static boolean isValidTree(List<int[]> edges, Set<Integer> nodes) {
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

    public String parseContentToGraphFormat(String content, long userId) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // 解析 content: [[[1,2],[2,3]],[1,2,3]]
            // 第一个数组是edges，第二个数组是nodes
            List<List<Object>> contentData = mapper.readValue(content, new TypeReference<>() {});

            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, String>> edges = new ArrayList<>();
            List<Map<String, Object>> nodes = new ArrayList<>();

            if (contentData.size() >= 2) {
                // 解析edges - 格式: [[1,2],[2,3]]
                List<Object> edgeDataRaw = contentData.get(0);
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

                // 解析nodes - 格式: [1,2,3]
                List<Object> nodeIdsRaw = contentData.get(1);
                List<Long> nodeIds = new ArrayList<>();
                for (Object nodeIdObj : nodeIdsRaw) {
                    if (nodeIdObj instanceof Number) {
                        nodeIds.add(((Number) nodeIdObj).longValue());
                    }
                }

                // 获取课程名称
                Map<Long, String> courseNames = getCourseNames(nodeIds);

                // 批量查询用户对这些课程的学习进度
                Map<Long, UserCourseDO> userCourseMap = userCourseService.getUserCoursesBatch(userId, new ArrayList<>(nodeIds));

                for (long nodeId : nodeIds) {
                    String courseName = courseNames.getOrDefault(nodeId, "课程" + nodeId);

                    UserCourseDO userCourse = userCourseMap.get((long)nodeId);
                    boolean finished = userCourse != null && userCourse.getProgressPercent() >= 10000;
                    double progress = userCourse != null ? userCourse.getProgressPercent() / 100.0 : 0.0;

                    Map<String, Object> nodeMap = new HashMap<>();
                    nodeMap.put("id", String.valueOf(nodeId));
                    nodeMap.put("name", courseName);
                    nodeMap.put("finished", finished);
                    nodeMap.put("progress", progress);
                    nodes.add(nodeMap);
                }
            }

            graphData.put("edges", edges);
            graphData.put("nodes", nodes);

            return mapper.writeValueAsString(graphData);
        } catch (Exception e) {
            log.error("Failed to parse content to graph format", e);
            throw new RuntimeException("Content parsing failed", e);
        }
    }

    private Map<Long, String> getCourseNames(List<Long> courseIds) {
        Map<Long, String> courseNames = new HashMap<>();
        if (courseIds.isEmpty()) {
            return courseNames;
        }

        try {
            List<CourseDO> courses = courseMapper.getByIds(courseIds);
            for (CourseDO course : courses) {
                courseNames.put(course.getId(), course.getName());
            }
        } catch (Exception e) {
            log.error("Failed to get course names for courseIds: {}", courseIds, e);
            // 如果查询失败，使用默认名称
            for (long id : courseIds) {
                courseNames.put(id, "课程" + id);
            }
        }

        return courseNames;
    }
}
