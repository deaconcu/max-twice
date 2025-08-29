package com.prosper.learn.domain.service.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.prosper.learn.common.UnionFind;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.RoadmapDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.dataobject.UserProfileDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import com.prosper.learn.persistence.mapper.UserProfileMapper;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CourseMapper courseMapper;
    private final RoadmapMapper roadmapMapper;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserRoadmapMapper userRoadmapMapper;
    private final UpvoteService upvoteService;
    private final UserCourseService userCourseService;
    private final ScoreCalculationService scoreCalculationService;

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

    /**
     * 获取职业路线图列表（带置顶和状态信息）
     */
    public List<RoadmapDTO> getRoadmapsByProfession(Long professionId, Long lastId, long userId) {
        List<RoadmapDO> roadmapList = new ArrayList<>();
        int limit = 20;

        List<Long> pinnedRoadmapIds = new ArrayList<>();
        if (lastId == null || lastId == 0) {
            UserProfileDO userProfile = userProfileMapper.getById(userId);

            if (userProfile != null && userProfile.getRoadmapPin() != null) {
                Map<String, List<Long>> pinMap = null;
                try {
                    pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
                } catch (JsonProcessingException e) {
                    throw ErrorCode.SYSTEM_ERROR.exception(e);
                }

                List<Long> professionPins = pinMap.get(professionId.toString());
                if (professionPins != null && !professionPins.isEmpty()) {
                    pinnedRoadmapIds = professionPins;
                }
            }

            if (!pinnedRoadmapIds.isEmpty()) {
                List<RoadmapDO> pinnedRoadmaps = roadmapMapper.getByIds(pinnedRoadmapIds);
                roadmapList.addAll(pinnedRoadmaps);
            }

            int remainingLimit = limit - roadmapList.size();
            if (remainingLimit > 0) {
                List<RoadmapDO> otherRoadmaps = roadmapMapper.getListByProfessionExcludingOrderByScore(
                    professionId, 0, remainingLimit, pinnedRoadmapIds);
                roadmapList.addAll(otherRoadmaps);
            }
        } else {
            RoadmapDO lastRoadmap = roadmapMapper.get(lastId);
            if (lastRoadmap != null) {
                roadmapList = roadmapMapper.getListByProfessionAfterScoreExcluding(
                        professionId, lastRoadmap.getScore(), lastId, limit, null);
            }
        }

        List<RoadmapDTO> dtoList = Converter.INSTANCE.toRoadMapDTO(roadmapList);

        if (!dtoList.isEmpty()) {
            List<Long> roadmapIds = dtoList.stream()
                .map(RoadmapDTO::getId)
                .collect(Collectors.toList());

            Set<Long> upvotedIds = upvoteService.getUpvotedRoadmapIds(roadmapIds, userId);

            Set<Long> pinnedIds = new HashSet<>();
            if (lastId == null || lastId == 0) {
                pinnedIds.addAll(pinnedRoadmapIds);
            } else {
                UserProfileDO userProfile = userProfileMapper.getById(userId);
                if (userProfile != null && userProfile.getRoadmapPin() != null) {
                    Map<String, List<Long>> pinMap = null;
                    try {
                        pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        throw ErrorCode.SYSTEM_ERROR.exception(e);
                    }
                    List<Long> professionPins = pinMap.get(professionId.toString());
                    if (professionPins != null) {
                        pinnedIds.addAll(professionPins);
                    }
                }
            }

            List<Long> learningRoadmapIds = userRoadmapMapper.getBatchLearningStatus(userId, roadmapIds);
            Set<Long> learningIds = new HashSet<>(learningRoadmapIds);

            for (RoadmapDTO dto : dtoList) {
                dto.setUpvoted(upvotedIds.contains(dto.getId()));
                dto.setPinned(pinnedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));
            }
        }

        for (RoadmapDTO dto : dtoList) {
            if (dto.getContent() != null) {
                String formattedContent = parseContentToGraphFormat(dto.getContent(), userId);
                dto.setContent(formattedContent);
            }
        }

        return dtoList;
    }

    /**
     * 更新路线图
     */
    @Transactional
    public void updateRoadmap(Long id, String content, long userId) {
        if (!isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());
        if (roadmapDO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        if (roadmapDO.getCreatorId() != userId) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapMapper.update(roadmapDO);
    }

    /**
     * 路线图点赞
     */
    @Transactional
    public RoadmapDTO upvoteRoadmap(Long id, long userId) {
        boolean voted = upvoteService.upvoteRoadmap(id.intValue(), (int)userId);

        int voteDelta = voted ? 1 : -1;
        roadmapMapper.updateVoteCount(id.intValue(), voteDelta);

        RoadmapDO roadmapDO = roadmapMapper.get(id.intValue());

        scoreCalculationService.checkAndUpdateRoadmapScore(roadmapDO);
        roadmapDO = roadmapMapper.get(id.intValue());

        RoadmapDTO roadmapDTO = Converter.INSTANCE.toRoadMapDTO(roadmapDO);
        roadmapDTO.setUpvoted(voted);

        return roadmapDTO;
    }

    /**
     * 创建路线图
     */
    @Transactional
    public Long createRoadmap(Long professionId, String content, String description, long userId) {
        if (!isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setProfessionId(professionId);
        roadmapDO.setCreatorId(userId);
        roadmapDO.setContent(content);
        roadmapDO.setDescription(description);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setVote(0);
        roadmapDO.setComment(0);
        roadmapDO.setCreatedAt(LocalDateTime.now());
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapMapper.insert(roadmapDO);
        return roadmapDO.getId();
    }

    /**
     * 获取路线图详情（带格式化内容）
     */
    public RoadmapDTO getRoadmapWithContent(Long id, long userId) {
        RoadmapDTO roadmapDTO = getById(id.intValue(), (int)userId);

        if (roadmapDTO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        if (roadmapDTO.getContent() != null) {
            String formattedContent = parseContentToGraphFormat(roadmapDTO.getContent(), userId);
            roadmapDTO.setContent(formattedContent);
        }

        return roadmapDTO;
    }

    /**
     * 置顶/取消置顶路线图
     */
    @Transactional
    public String pinRoadmap(Long professionId, Long roadmapId, long userId) {
        UserProfileDO userProfile = userProfileMapper.getById(userId);
        Map<String, List<Long>> pinMap = new HashMap<>();

        if (userProfile != null && userProfile.getRoadmapPin() != null) {
            try {
                pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw ErrorCode.SYSTEM_ERROR.exception(e);
            }
        }

        String professionKey = String.valueOf(professionId);
        List<Long> professionPins = pinMap.getOrDefault(professionKey, new ArrayList<>());

        boolean isPinned = professionPins.contains(roadmapId);
        String message;

        if (isPinned) {
            professionPins.remove(roadmapId);
            message = "unpinned";
        } else {
            if (professionPins.size() >= 19) {
                throw ErrorCode.ROADMAP_PIN_LIMIT_EXCEEDED.exception();
            }
            professionPins.add(roadmapId);
            message = "pinned";
        }

        if (professionPins.isEmpty()) {
            pinMap.remove(professionKey);
        } else {
            pinMap.put(professionKey, professionPins);
        }

        String updatedPinJson = null;
        try {
            updatedPinJson = objectMapper.writeValueAsString(pinMap);
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        if (userProfile == null) {
            userProfile = new UserProfileDO();
            userProfile.setUserId(userId);
            userProfile.setRoadmapPin(updatedPinJson);
            userProfile.setSubscription("");
            userProfileMapper.insert(userProfile);
        } else {
            userProfileMapper.updateRoadmapPin(userId, updatedPinJson);
        }

        return message;
    }
}
