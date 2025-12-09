package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.prosper.learn.application.converter.RoadmapConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.ProfessionDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserRoadmapDataService;
import com.prosper.learn.shared.common.utils.UnionFind;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserProfileDO;
import com.prosper.learn.user.profile.UserProfileDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CourseDataService courseDataService;
    private final RoadmapDataService roadmapDataService;
    private final UserDataService userDataService;
    private final UserProfileDataService userProfileDataService;
    private final UserRoadmapDataService userRoadmapDataService;
    private final ProfessionDataService professionDataService;
    private final UpvoteService upvoteService;
    private final UserCourseService userCourseService;
    private final ScoreCalculationService scoreCalculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoadmapConverter roadmapConverter;
    private final UserConverter userConverter;
    private final ProfessionService professionService;
    private final SystemProperties systemProperties;

    // ========== 常量定义 ==========
    
    private static final String DEFAULT_EMPTY_STRING = "";
    
    // ========== DTO转换方法 ==========
    
    /**
     * 转换单个对象为摘要DTO
     */
    public RoadmapSummaryDTO toSummaryDTO(RoadmapDO roadmapDO) {
        return roadmapConverter.toSummaryDTO(roadmapDO);
    }

    /**
     * 转换列表为摘要DTO列表
     */
    public List<RoadmapSummaryDTO> toSummaryDTO(List<RoadmapDO> roadmapDOList) {
        return roadmapConverter.toSummaryDTO(roadmapDOList);
    }

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + profession + upvoted + formatted content
     */
    public RoadmapWithStatusDTO toRoadmapWithStatus(RoadmapDO roadmapDO, long userId) {
        if (roadmapDO == null) return null;

        RoadmapWithStatusDTO dto = roadmapConverter.toWithStatusDTO(roadmapDO);

        // 设置创建者信息
        if (roadmapDO.getCreatorId() != null) {
            dto.setCreator(userConverter.toBriefDTO(userDataService.getById(roadmapDO.getCreatorId())));
        }

        // 设置专业信息
        if (roadmapDO.getProfessionId() != null) {
            dto.setProfession(professionService.toDTO(professionDataService.getById(roadmapDO.getProfessionId())));
        }

        // 设置点赞状态
        dto.setUpvoted(upvoteService.hasUpvoted(roadmapDO.getId(), ContentType.roadmap, userId));

        // 设置格式化内容
        if (roadmapDO.getContent() != null) {
            dto.setContent(parseContentToGraphFormat(roadmapDO.getContent(), userId));
        }

        return dto;
    }

    /**
     * 转换为路线图（包含完整业务信息）
     * 包含：creator + profession + upvoted + pinned + learning + formatted content
     */
    private List<RoadmapWithStatusDTO> toRoadmapWithFullInfo(List<RoadmapDO> roadmapList, long userId, Long professionId, Long lastId, List<Long> pinnedRoadmapIds) {
        List<RoadmapWithStatusDTO> dtoList = roadmapConverter.toWithStatusDTO(roadmapList);

        if (!dtoList.isEmpty()) {
            List<Long> roadmapIds = dtoList.stream()
                    .map(RoadmapSummaryDTO::getId)
                    .collect(Collectors.toList());

            UserBriefDTO userDTO = userConverter.toBriefDTO(userDataService.getById(userId));
            ProfessionDTO professionDTO = professionService.getById(professionId, true);
            Set<Long> upvotedIds = upvoteService.getUpvotedIds(roadmapIds, ContentType.roadmap, userId);
            Set<Long> pinnedIds = getPinnedIdsForCurrentRequest(userId, professionId, lastId, pinnedRoadmapIds);
            Set<Long> learningIds = getLearningIds(userId, roadmapIds);

            for (RoadmapWithStatusDTO dto : dtoList) {
                dto.setProfession(professionDTO);
                dto.setCreator(userDTO);
                dto.setUpvoted(upvotedIds.contains(dto.getId()));
                dto.setPinned(pinnedIds.contains(dto.getId()));
                dto.setLearning(learningIds.contains(dto.getId()));

                if (dto.getContent() != null) {
                    String formattedContent = parseContentToGraphFormat(dto.getContent(), userId);
                    dto.setContent(formattedContent);
                }
            }
        }
        return dtoList;
    }

    // ========== 公共方法 ==========

    /**
     * 获取职业路线图列表（公开接口，无个性化信息）
     * 用于匿名用户浏览
     */
    public List<RoadmapSummaryDTO> getRoadmapsByProfessionPublic(Long professionId, Long lastId, Integer pageSize) {
        validateProfessionId(professionId);

        int limit = pageSize != null && pageSize > 0 ? pageSize : systemProperties.getRoadmap().getDefaultPageSize();
        List<RoadmapDO> roadmapList;

        if (lastId == null || lastId == 0) {
            roadmapList = roadmapDataService.getListByProfessionExcludingOrderByScore(
                professionId, limit, new ArrayList<>());
        } else {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                roadmapList = roadmapDataService.getListByProfessionAfterScoreExcluding(
                    professionId, lastRoadmap.getScore(), lastId, limit, null);
            } else {
                roadmapList = new ArrayList<>();
            }
        }

        // 转换为DTO，只包含基础信息
        return toSummaryDTO(roadmapList);
    }

    /**
     * return RoadmapDTO
     */
    public RoadmapSummaryDTO getById(long id) {
        validateRoadmapId(id);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        if (roadmapDO == null) {
            return null;
        }
        return toSummaryDTO(roadmapDO);
    }

    /**
     * return RoadmapWithStatusDTO (替代旧的 v1)
     */
    public RoadmapWithStatusDTO getById(long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);

        RoadmapDO roadmapDO = roadmapDataService.getById(id);
        if (roadmapDO == null) {
            return null;
        }
        return toRoadmapWithStatus(roadmapDO, userId);
    }

    /**
     * 标准化content内容并计算hash值
     * @param content 原始content字符串，格式: [[[1,2],[2,3]],[1,2,3]]
     * @return 标准化后的hash值
     */
    public static String calculateContentHash(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!rootNode.isArray() || rootNode.size() != 2) {
                throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
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
        validateContent(content);
        validateUserId(userId);
        
        try {
            List<List<Object>> contentData = objectMapper.readValue(content, new TypeReference<>() {});

            Map<String, Object> graphData = new HashMap<>();
            List<Map<String, String>> edges = new ArrayList<>();
            List<Map<String, Object>> nodes = new ArrayList<>();

            if (contentData.size() >= 2) {
                edges = parseEdges(contentData.get(0));
                nodes = parseNodes(contentData.get(1), userId);
            }

            graphData.put("edges", edges);
            graphData.put("nodes", nodes);

            return objectMapper.writeValueAsString(graphData);
        } catch (Exception e) {
            log.error("内容解析为图形格式失败", e);
            throw ErrorCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    private Map<Long, String> getCourseNames(List<Long> courseIds) {
        Map<Long, String> courseNames = new HashMap<>();
        if (courseIds.isEmpty()) {
            return courseNames;
        }

        try {
            List<CourseDO> courses = courseDataService.getByIds(courseIds);
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
    public List<RoadmapWithStatusDTO> getRoadmapsByProfession(Long professionId, Long lastId, UserDO currentUser) {
        validateProfessionId(professionId);

        List<RoadmapDO> roadmapList = new ArrayList<>();
        int limit = systemProperties.getRoadmap().getDefaultPageSize();

        List<Long> pinnedRoadmapIds = new ArrayList<>();
        if (lastId == null) {
            pinnedRoadmapIds = getPinnedRoadmapIds(currentUser.getId(), professionId);

            if (!pinnedRoadmapIds.isEmpty()) {
                List<RoadmapDO> pinnedRoadmaps = roadmapDataService.getByIds(pinnedRoadmapIds);
                roadmapList.addAll(pinnedRoadmaps);
            }

            int remainingLimit = limit - roadmapList.size();
            if (remainingLimit > 0) {
                List<RoadmapDO> otherRoadmaps = roadmapDataService.getListByProfessionExcludingOrderByScore(
                    professionId, remainingLimit, pinnedRoadmapIds);
                roadmapList.addAll(otherRoadmaps);
            }
        } else {
            RoadmapDO lastRoadmap = roadmapDataService.getById(lastId);
            if (lastRoadmap != null) {
                roadmapList = roadmapDataService.getListByProfessionAfterScoreExcluding(
                        professionId, lastRoadmap.getScore(), lastId, limit, null);
            }
        }

        return toRoadmapWithFullInfo(roadmapList, currentUser.getId(), professionId, lastId, pinnedRoadmapIds);
    }

    /**
     * 获取用户创建的路线图列表（所有状态）
     * @param userId 用户ID
     * @param lastId 分页游标
     * @return 路线图列表
     */
    public List<RoadmapSummaryDTO> getUserRoadmaps(Long userId, Long lastId, ContentState state) {
        validateUserId(userId);

        int limit = systemProperties.getRoadmap().getDefaultPageSize();
        List<RoadmapDO> roadmapList = roadmapDataService.getListByCreatorWithPaging(
                userId, lastId, limit, state == null ? null : state.value());
        return toSummaryDTO(roadmapList);
    }

    /**
     * 删除路线图（软删除）
     * @param id 路线图ID
     * @param operator 操作用户
     */
    @Transactional
    public void deleteRoadmap(Long id, UserDO operator) {
        validateRoadmapId(id);

        RoadmapDO roadmapDO = validateRoadmapExists(id);

        // 验证权限：只能删除自己创建的路线图，除非是管理员
        if (!roadmapDO.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        int result = roadmapDataService.softDelete(id);
        if (result == 0) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }
    }

    /**
     * 更新路线图
     */
    @Transactional
    public void updateRoadmap(Long id, String content, UserDO operator) {
        validateRoadmapId(id);
        validateContent(content);

        if (systemProperties.getRoadmap().isEnableContentValidation() && !isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = validateRoadmapExists(id);

        // 验证权限：只有所有者或管理员可以修改
        if (!roadmapDO.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw ErrorCode.PERMISSION_DENIED.exception();
        }

        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setUpdatedAt(LocalDateTime.now());

        roadmapDataService.update(roadmapDO);
    }

    /**
     * 创建路线图
     */
    @Transactional
    public Long createRoadmap(Long professionId, String content, String description, long userId) {
        validateProfessionId(professionId);
        validateContent(content);
        validateUserId(userId);
        
        if (systemProperties.getRoadmap().isEnableContentValidation() && !isValidContentFormat(content)) {
            throw ErrorCode.ROADMAP_CONTENT_INVALID.exception();
        }

        RoadmapDO roadmapDO = new RoadmapDO();
        roadmapDO.setContent(content);
        roadmapDO.setContentHash(calculateContentHash(content));
        roadmapDO.setDescription(description);
        roadmapDO.setProfessionId(professionId);
        roadmapDO.setCreatorId(userId);

        roadmapDataService.insert(roadmapDO);
        return roadmapDO.getId();
    }

    /**
     * 获取路线图详情（带格式化内容）
     */
    public RoadmapWithStatusDTO getRoadmapWithContent(Long id, long userId) {
        validateRoadmapId(id);
        validateUserId(userId);

        RoadmapWithStatusDTO roadmapDTO = getById(id, userId);

        if (roadmapDTO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        return roadmapDTO;
    }

    /**
     * 置顶/取消置顶路线图
     */
    @Transactional
    public Boolean pinRoadmap(Long professionId, Long roadmapId, long userId) {
        UserProfileDO userProfile = userProfileDataService.getById(userId);
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
        Boolean pinned;

        if (isPinned) {
            professionPins.remove(roadmapId);
            pinned = false;
        } else {
            if (professionPins.size() >= 19) {
                throw ErrorCode.ROADMAP_PIN_LIMIT_EXCEEDED.exception();
            }
            professionPins.add(roadmapId);
            pinned = true;
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
            userProfileDataService.insert(userProfile);
        } else {
            userProfileDataService.updateRoadmapPin(userId, updatedPinJson);
        }

        return pinned;
    }
    
    // ========== 私有辅助方法 ==========
    
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateRoadmapId(Long roadmapId) {
        if (roadmapId == null || roadmapId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateProfessionId(Long professionId) {
        if (professionId == null || professionId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private RoadmapDO validateRoadmapExists(Long roadmapId) {
        RoadmapDO roadmapDO = roadmapDataService.getById(roadmapId);
        if (roadmapDO == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }
        return roadmapDO;
    }
    
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
    
    private List<Map<String, Object>> parseNodes(List<Object> nodeIdsRaw, long userId) {
        List<Long> nodeIds = new ArrayList<>();
        for (Object nodeIdObj : nodeIdsRaw) {
            if (nodeIdObj instanceof Number) {
                nodeIds.add(((Number) nodeIdObj).longValue());
            }
        }

        Map<Long, String> courseNames = getCourseNames(nodeIds);
        Map<Long, UserCourseDO> userCourseMap = systemProperties.getRoadmap().isEnableBatchStatusQuery()
            ? userCourseService.getUserCoursesBatch(userId, new ArrayList<>(nodeIds))
            : new HashMap<>();

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (long nodeId : nodeIds) {
            String courseName = courseNames.getOrDefault(nodeId, "课程" + nodeId);

            UserCourseDO userCourse = userCourseMap.get(nodeId);
            boolean finished = userCourse != null && userCourse.getProgressPercent() >= systemProperties.getRoadmap().getCompletionThreshold();
            double progress = userCourse != null ? userCourse.getProgressPercent() / systemProperties.getRoadmap().getProgressPrecisionDivisor() : 0.0;

            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", String.valueOf(nodeId));
            nodeMap.put("name", courseName);
            nodeMap.put("finished", finished);
            nodeMap.put("progress", progress);
            nodes.add(nodeMap);
        }
        
        return nodes;
    }
    
    private List<Long> getPinnedRoadmapIds(long userId, Long professionId) {
        UserProfileDO userProfile = userProfileDataService.getById(userId);
        if (userProfile == null || userProfile.getRoadmapPin() == null) {
            return new ArrayList<>();
        }
        
        try {
            Map<String, List<Long>> pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
            List<Long> professionPins = pinMap.get(professionId.toString());
            return professionPins != null ? professionPins : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("解析置顶数据失败", e);
            return new ArrayList<>();
        }
    }
    

    
    private Set<Long> getPinnedIdsForCurrentRequest(long userId, Long professionId, Long lastId, List<Long> pinnedRoadmapIds) {
        Set<Long> pinnedIds = new HashSet<>();
        if (lastId == null || lastId == 0) {
            pinnedIds.addAll(pinnedRoadmapIds);
        } else {
            List<Long> currentPinnedIds = getPinnedRoadmapIds(userId, professionId);
            pinnedIds.addAll(currentPinnedIds);
        }
        return pinnedIds;
    }
    
    private Set<Long> getLearningIds(long userId, List<Long> roadmapIds) {
        if (systemProperties.getRoadmap().isEnableBatchStatusQuery()) {
            List<Long> learningRoadmapIds = userRoadmapDataService.getBatchLearningStatus(userId, roadmapIds);
            return new HashSet<>(learningRoadmapIds);
        }
        return new HashSet<>();
    }
    
    private Map<String, List<Long>> parseExistingPinMap(UserProfileDO userProfile) {
        Map<String, List<Long>> pinMap = new HashMap<>();
        if (userProfile != null && userProfile.getRoadmapPin() != null) {
            try {
                pinMap = objectMapper.readValue(userProfile.getRoadmapPin(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                log.error("解析置顶数据失败", e);
                throw ErrorCode.JSON_PROCESSING_ERROR.exception(e);
            }
        }
        return pinMap;
    }
    
    private void updatePinMap(Map<String, List<Long>> pinMap, String professionKey, List<Long> professionPins, long userId, UserProfileDO userProfile) {
        if (professionPins.isEmpty()) {
            pinMap.remove(professionKey);
        } else {
            pinMap.put(professionKey, professionPins);
        }

        try {
            String updatedPinJson = objectMapper.writeValueAsString(pinMap);
            
            if (userProfile == null) {
                userProfile = new UserProfileDO();
                userProfile.setUserId(userId);
                userProfile.setRoadmapPin(updatedPinJson);
                userProfile.setSubscription(DEFAULT_EMPTY_STRING);
                userProfileDataService.insert(userProfile);
            } else {
                userProfileDataService.updateRoadmapPin(userId, updatedPinJson);
            }
        } catch (JsonProcessingException e) {
            log.error("更新置顶数据失败", e);
            throw ErrorCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    // ========== Admin管理方法 ==========

    /**
     * Admin管理：按条件获取路线图列表
     */
    public List<RoadmapSummaryDTO> listByFilter(Byte state, Long professionId, Long creatorId, Long lastId) {
        List<RoadmapDO> roadmapDOList = roadmapDataService.listByFilter(state, professionId, creatorId, lastId);
        return toSummaryDTO(roadmapDOList);
    }

    /**
     * 批准路线图（直接通过，保留描述）
     */
    public RoadmapSummaryDTO approve(long id, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        Utils.validateStateTransition(roadmap.getState(), ContentState.PUBLISHED);

        // 获取职业信息
        ProfessionDTO profession = professionService.getById(roadmap.getProfessionId(), false);

        roadmapDataService.approve(id);
        roadmap.setState(ContentState.PUBLISHED.value());

        // 发布审核通过事件，触发统计更新（不发送消息）
        eventPublisher.publishEvent(ContentApprovedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            profession != null ? profession.getId() : null,
            profession != null ? profession.getName() : null
        ));

        return toSummaryDTO(roadmap);
    }

    /**
     * 拒绝路线图
     */
    public RoadmapSummaryDTO reject(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        Utils.validateStateTransition(roadmap.getState(), ContentState.REJECTED);

        // 获取职业信息用于通知
        ProfessionDTO profession = professionService.getById(roadmap.getProfessionId(), false);

        roadmapDataService.reject(id, reason);
        roadmap.setState(ContentState.REJECTED.value());

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forRoadmap(
            roadmap.getCreatorId(),
            roadmap.getId(),
            profession != null ? profession.getId() : null,
            profession != null ? profession.getName() : null,
            reason
        ));

        return toSummaryDTO(roadmap);
    }

    /**
     * 封禁路线图
     */
    public RoadmapSummaryDTO ban(long id, String reason, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        Utils.validateStateTransition(roadmap.getState(), ContentState.BANNED);

        roadmapDataService.ban(id, reason);
        roadmap.setState(ContentState.BANNED.value());

        // ban 不发送任何消息或事件
        log.info("路线图 {} 被封禁，操作者: {}, 原因: {}", id, operator.getId(), reason);

        return toSummaryDTO(roadmap);
    }

    /**
     * 清除描述并批准路线图
     */
    public RoadmapSummaryDTO approveAndClearDescription(long id, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        roadmap.setDescription("");
        roadmap.setState(ContentState.PUBLISHED.value());
        roadmapDataService.update(roadmap);
        return toSummaryDTO(roadmap);
    }

    /**
     * 更新路线图描述（管理员操作）
     */
    public RoadmapSummaryDTO updateDescription(long id, String description, UserDO operator) {
        RoadmapDO roadmap = roadmapDataService.getById(id);
        if (roadmap == null) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }

        roadmap.setDescription(description != null ? description : "");
        roadmapDataService.update(roadmap);
        return toSummaryDTO(roadmap);
    }
}
