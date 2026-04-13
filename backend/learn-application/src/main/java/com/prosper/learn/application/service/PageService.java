package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.assembler.CourseAssembler;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.converter.PostConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.NodeTocDTO;
import com.prosper.learn.application.dto.response.course.CourseSummaryDTO;
import com.prosper.learn.application.dto.response.course.CourseFullDTO;
import com.prosper.learn.application.dto.response.node.NodeSimpleDTO;
import com.prosper.learn.application.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.application.dto.response.post.PostWithVoteDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.interaction.ContentViewedEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageService {

    private final TocDomainService tocService;
    private final LearningProgressService learningProgressService;
    private final NodeDataService nodeDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PostService postService;
    private final PostDataService postDataService;
    private final UpvoteDataService upvoteDataService;
    private final CourseDataService courseDataService;
    private final UserDataService userDataService;
    private final CommentDataService commentDataService;
    private final UserLearningDomainService userLearningDomainService;
    private final SystemProperties systemProperties;
    private final NodeConverter nodeConverter;
    private final PostConverter postConverter;
    private final UserConverter userConverter;
    private final CourseService courseService;
    private final CourseAssembler courseAssembler;
    private final UserLearningService userLearningService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContentStatsDomainService contentStatsDomainService;
    private final ContentVisibilityService contentVisibilityService;


    // ========== 常量定义 ==========

    private static final String DEFAULT_PATH_PREFIX = "1-";
    private static final String PATH_SEPARATOR = "-";
    private static final String CHOSEN_POST_FIELD = "+";
    private static final Pattern PATH_PATTERN = Pattern.compile("^\\d+(\\-\\d+)*$");

    
    // ========== 公共方法 ==========

    public Utils.Pair<String, Map<Long, NodeWithProgressDTO>> getToc(long userId, long nodeId, boolean create) {
        ArrayNode arrayNode = tocService.getToc(userId, nodeId, create);

        // 如果目录为空（用户未初始化），返回空结构
        if (arrayNode == null) {
            return new Utils.Pair<>("[]", new HashMap<>());
        }

        Set<Long> keys = new HashSet<>();
        Utils.collectKeys(arrayNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeDataService.getByIds(keys.stream().toList());

        // 批量检查这些节点中哪些已完成（只查询需要的节点，而不是用户所有完成的节点）
        Set<Long> completedNodes = keys.isEmpty() ? Set.of() : learningProgressService.getCompletedNodesInList(userId, keys);

        // 构建包含完成状态的节点信息
        Map<Long, NodeWithProgressDTO> nodeInfos = nodeList.stream()
                .collect(Collectors.toMap(
                        NodeDO::getId,
                        node -> nodeConverter.toWithProgressDTO(node, completedNodes.contains(node.getId()))
                ));


        return new Utils.Pair<>(arrayNode.toString(), nodeInfos);
    }

    /**
     * 获取TOC（公开版本，无需userId）
     * 所有节点的完成状态都为false
     */
    public Utils.Pair<String, Map<Long, NodeWithProgressDTO>> getTocPublic(long courseId) {
        // 获取课程的根节点ID
        CourseDO course = courseDataService.validateAndGet(courseId);

        ArrayNode arrayNode = tocService.getToc(0L, course.getRootNodeId(), true);

        Set<Long> keys = new HashSet<>();
        Utils.collectKeys(arrayNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeDataService.getByIds(keys.stream().toList());

        // 所有节点都标记为未完成
        Map<Long, NodeWithProgressDTO> nodeInfos = nodeList.stream()
                .collect(Collectors.toMap(
                        NodeDO::getId,
                        node -> nodeConverter.toWithProgressDTO(node, false)
                ));

        return new Utils.Pair<>(arrayNode.toString(), nodeInfos);
    }

    /**
     * 根据评论ID读取页面数据
     */
    public Map<String, Object> readPageByComment(Long commentId, long userId) {
        CommentDO commentDO = commentDataService.validateAndGet(commentId);

        // 检查评论及其祖先链的可见性
        contentVisibilityService.validateVisibility(ContentType.comment, commentId, userId);

        if (commentDO.getReplyToCommentId() != 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("commentId", commentDO.getReplyToCommentId());
            data.put("subCommentId", commentDO.getId());
            return data;
        }

        PostDO postDO = null;
        NodeDO nodeDO;
        CourseDO courseDO;
        String path;

        if (commentDO.getObjectType() == ContentType.post.value()) {
            long postId = commentDO.getObjectId();
            postDO = postService.validateAndGetPost(postId);

            // 发布内容浏览事件
            eventPublisher.publishEvent(new ContentViewedEvent(
                userId,
                postDO.getId(),
                ContentType.post,
                postDO.getCreatorId()
            ));

            nodeDO = nodeDataService.validateAndGet(postDO.getNodeId());
            courseDO = validateCourseExists(nodeDO.getCourseId());
            path = generateDefaultPath(courseDO.getRootNodeId());
        } else {
            nodeDO = nodeDataService.validateAndGet(commentDO.getObjectId());
            courseDO = validateCourseExists(nodeDO.getCourseId());
            path = generateDefaultPath(courseDO.getRootNodeId());
        }

        return readPageData(courseDO, path, nodeDO, postDO, userId);
    }

    /**
     * 根据帖子ID读取页面数据
     */
    public Map<String, Object> readPageByPost(Long postId, long userId) {
        PostDO postDO = postService.validateAndGetPost(postId);

        // 检查帖子及其祖先链的可见性
        contentVisibilityService.validateVisibility(ContentType.post, postId, userId);

        // 发布内容浏览事件
        eventPublisher.publishEvent(new ContentViewedEvent(
            userId,
            postDO.getId(),
            ContentType.post,
            postDO.getCreatorId()
        ));

        NodeDO nodeDO = nodeDataService.validateAndGet(postDO.getNodeId());
        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
        String path = generateDefaultPath(courseDO.getRootNodeId());

        return readPageData(courseDO, path, nodeDO, postDO, userId);
    }

    /**
     * 根据节点ID和路径读取页面数据（旧实现，已废弃）
     */
//    public Map<String, Object> readPageByNode(Long nodeId, String path, long userId) {
//        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);
//        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
//
//        // 如果没有提供 path，使用节点ID作为根路径
//        if (path == null || path.isEmpty()) {
//            path = generateDefaultPath(nodeId);
//        }
//
//        return readPageData(courseDO, path, nodeDO, null, userId);
//    }

    /**
     * 根据节点ID和路径读取页面数据（新实现）
     * 以 Node 为中心，支持通过 path 访问子节点
     */
    public Map<String, Object> readPageByNode(Long nodeId, String path, long userId) {
        NodeDO rootNodeDO = nodeDataService.validateAndGet(nodeId);

        // 检查节点及其祖先链的可见性
        contentVisibilityService.validateVisibility(ContentType.node, nodeId, userId);

        CourseDO courseDO = validateCourseExists(rootNodeDO.getCourseId());

        // 获取以 nodeId 为根的 TOC（目录树），不自动创建
        Utils.Pair<String, Map<Long, NodeWithProgressDTO>> tocResponse = getToc(userId, nodeId, false);
        NodeTocDTO nodeTocDTO = new NodeTocDTO(tocResponse.left(), tocResponse.right());

        JsonNode rootNode = parseJsonSafely(nodeTocDTO.getContents());

        // 处理路径
        path = sanitizePath(path, nodeId);

        // 根据 path 定位到目标节点
        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, nodeId);

        // 如果目录为空，pair 会是 null，直接使用根节点
        NodeDO targetNodeDO;
        JsonNode targetNodeJson = null;
        PostDO chosenPosting = null;

        if (pair == null) {
            // 目录为空，使用根节点
            targetNodeDO = rootNodeDO;
        } else {
            long targetNodeId = pair.left();
            targetNodeJson = pair.right();
            targetNodeDO = nodeDataService.validateAndGet(targetNodeId);

            // 检查目标节点的可见性
            contentVisibilityService.validateVisibility(ContentType.node, targetNodeId, userId);

            // 从目录中提取选中的帖子
            List<Long> chosenUserIds = new LinkedList<>();
            chosenPosting = extractChosenPosting(targetNodeJson, chosenUserIds);
        }

        // 获取帖子列表
        List<Long> userIds = new LinkedList<>();
        if (chosenPosting != null) {
            userIds.add(chosenPosting.getCreatorId());
        }
        List<PostDO> otherPostings = getOtherPostings(targetNodeDO.getId(), userIds);

        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        // 转换帖子为 DTO
        PostWithVoteDTO chosenPostingDTO = convertPostToDTO(chosenPosting, userMap, userId);
        List<PostWithVoteDTO> otherPostingsDTO = convertPostListToDTO(otherPostings, userMap, userId);

        long lastId = calculateLastId(chosenPosting, otherPostings);

        // 获取学习状态
        boolean learning = checkLearningStatus(userId, courseDO.getId());
        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, targetNodeDO.getId());
        Integer courseProgress = userLearningService.getProgress(userId, Enums.ContentType.node, courseDO.getRootNodeId());

        // 构建课程信息
        CourseFullDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        // 构建响应
        return buildPageDataResponse(nodeTocDTO, targetNodeDO, parentCourse, courseDO, subCourseList,
                chosenPostingDTO, otherPostingsDTO, lastId, path, userMap.values(),
                learning, null, nodeCompleted, courseProgress, nodeId, userId); // nodeId 是 TOC 的根节点
    }

    /**
     * 根据帖子ID读取页面数据（仅返回帖子详情页需要的数据）
     * 用于 PostDetailPage，不返回 TOC、fixedPostings、otherPostings
     */
    public Map<String, Object> readPageForPost(Long postId, long userId) {
        PostDO postDO = postService.validateAndGetPost(postId);

        // 发布内容浏览事件
        eventPublisher.publishEvent(new ContentViewedEvent(
            userId,
            postDO.getId(),
            ContentType.post,
            postDO.getCreatorId()
        ));

        NodeDO nodeDO = nodeDataService.validateAndGet(postDO.getNodeId());

        if (nodeDO.getState() != null && nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());

        // 构建用户信息
        List<Long> userIds = new LinkedList<>();
        userIds.add(postDO.getCreatorId());
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        // 转换帖子为 DTO
        PostWithVoteDTO postDTO = buildPostDTO(postDO, userMap, userId);

        // 获取学习状态
        boolean learning = checkLearningStatus(userId, courseDO.getId());
        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());
        Integer courseProgress = userLearningService.getProgress(userId, Enums.ContentType.node, courseDO.getRootNodeId());

        // 构建课程信息
        CourseFullDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        // 构建响应
        Map<String, Object> data = new HashMap<>();

        // 填充 node 统计数据
        NodeWithProgressDTO nodeDTO = nodeConverter.toWithProgressDTO(nodeDO, nodeCompleted);
        ContentStatsDTO nodeStats = contentStatsDomainService.getContentStats(ContentType.node, nodeDO.getId());
        nodeDTO.setCommentCount(nodeStats.getCommentCount());
        nodeDTO.setNodeReferenceCount(nodeStats.getNodeReferenceCount());

        data.put("node", nodeDTO);
        data.put("parentCourse", parentCourse);
        data.put("course", courseAssembler.toFullDTO(courseDO, userId));
        data.put("subCourseList", subCourseList);
        data.put("post", postDTO);
        data.put("users", new ArrayList<>(userMap.values()));
        data.put("learning", learning);

        return data;
    }

    /**
     * 根据评论ID读取页面数据（仅返回帖子详情页需要的数据）
     * 用于 PostDetailPage，不返回 TOC、fixedPostings、otherPostings
     * 特殊处理：如果评论是回复评论，返回重定向信息
     */
    public Map<String, Object> readPageForComment(Long commentId, long userId) {
        CommentDO commentDO = commentDataService.validateAndGet(commentId);

        // 特殊处理：回复评论返回重定向信息
        if (commentDO.getReplyToCommentId() != 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("commentId", commentDO.getReplyToCommentId());
            data.put("subCommentId", commentDO.getId());
            return data;
        }

        PostDO postDO = null;
        NodeDO nodeDO;
        CourseDO courseDO;

        if (commentDO.getObjectType() == ContentType.post.value()) {
            long postId = commentDO.getObjectId();
            postDO = postService.validateAndGetPost(postId);

            // 发布内容浏览事件
            eventPublisher.publishEvent(new ContentViewedEvent(
                userId,
                postDO.getId(),
                ContentType.post,
                postDO.getCreatorId()
            ));

            nodeDO = nodeDataService.validateAndGet(postDO.getNodeId());
        } else {
            nodeDO = nodeDataService.validateAndGet(commentDO.getObjectId());
        }

        if (nodeDO.getState() != null && nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        courseDO = validateCourseExists(nodeDO.getCourseId());

        // 构建用户信息
        List<Long> userIds = new LinkedList<>();
        if (postDO != null) {
            userIds.add(postDO.getCreatorId());
        }
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        // 转换帖子为 DTO
        PostWithVoteDTO postDTO = postDO != null ? buildPostDTO(postDO, userMap, userId) : null;

        // 获取学习状态
        boolean learning = checkLearningStatus(userId, courseDO.getId());
        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());
        Integer courseProgress = userLearningService.getProgress(userId, Enums.ContentType.node, courseDO.getRootNodeId());

        // 构建课程信息
        CourseFullDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        // 构建响应
        Map<String, Object> data = new HashMap<>();

        // 填充 node 统计数据
        NodeWithProgressDTO nodeDTO = nodeConverter.toWithProgressDTO(nodeDO, nodeCompleted);
        ContentStatsDTO nodeStats = contentStatsDomainService.getContentStats(ContentType.node, nodeDO.getId());
        nodeDTO.setCommentCount(nodeStats.getCommentCount());
        nodeDTO.setNodeReferenceCount(nodeStats.getNodeReferenceCount());

        data.put("node", nodeDTO);
        data.put("parentCourse", parentCourse);
        data.put("course", courseAssembler.toFullDTO(courseDO, userId));
        data.put("subCourseList", subCourseList);
        if (postDTO != null) {
            data.put("post", postDTO);
        }
        data.put("users", new ArrayList<>(userMap.values()));
        data.put("learning", learning);

        return data;
    }


    /**
     * 根据课程ID和路径读取页面数据
     * Course 是特殊的 Root Node，内部转换为使用 readPageByNode
     */
    public Map<String, Object> readPageByCourse(Long courseId, String path, long userId) {
        CourseDO courseDO = validateCourseExists(courseId);
        // Course 是 Root Node，使用 rootNodeId 访问
        return readPageByNode(courseDO.getRootNodeId(), path, userId);
    }

    /**
     * 根据课程ID和路径读取页面数据
     */
    public Map<String, Object> readPageByPath(Long courseId, String path, long userId) {
        validatePath(path);
        CourseDO courseDO = validateCourseExists(courseId);
        return readPageData(courseDO, path, null, null, userId);
    }

    /**
     * 根据课程ID和路径读取页面数据（公开接口，无需登录）
     * 用于匿名用户浏览，不包含个性化信息
     */
    public Map<String, Object> readPageByPathPublic(Long courseId, String path) {
        validatePath(path);
        CourseDO courseDO = validateCourseExists(courseId);
        return readPageDataPublic(courseDO, path);
    }

    /**
     * 核心页面数据聚合逻辑
     */
    private Map<String, Object> readPageData(CourseDO courseDO, String path, NodeDO nodeDO, PostDO postDO, long userId) {
        if (courseDO == null) {
            throw StatusCode.COURSE_NOT_FOUND.exception();
        }
        Utils.Pair<String, Map<Long, NodeWithProgressDTO>> response = getToc(userId, courseDO.getRootNodeId(), false);
        NodeTocDTO nodeTocDTO = new NodeTocDTO(response.left(), response.right());

        JsonNode rootNode = parseJsonSafely(nodeTocDTO.getContents());
        path = sanitizePath(path, courseDO.getRootNodeId());

        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, courseDO.getRootNodeId());

        List<Long> userIds = new LinkedList<>();
        PostDO chosenPosting = extractChosenPosting(pair.right(), userIds);

        if (nodeDO == null) {
            long nodeId = pair.left();
            nodeDO = nodeDataService.validateAndGet(nodeId);
        }

        if (nodeDO.getState() != null && nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        List<PostDO> otherPostings = getOtherPostings(nodeDO.getId(), userIds);
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        PostWithVoteDTO postDTO = buildPostDTO(postDO, userMap, userId);

        // 转换帖子为 DTO 并填充关联信息
        PostWithVoteDTO chosenPostingDTO = convertPostToDTO(chosenPosting, userMap, userId);
        List<PostWithVoteDTO> otherPostingsDTO = convertPostListToDTO(otherPostings, userMap, userId);

        long lastId = calculateLastId(chosenPosting, otherPostings);

        boolean learning = checkLearningStatus(userId, courseDO.getId());
        CourseFullDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());
        Integer courseProgress = userLearningService.getProgress(userId, Enums.ContentType.node, courseDO.getRootNodeId());

        return buildPageDataResponse(nodeTocDTO, nodeDO, parentCourse, courseDO, subCourseList,
                chosenPostingDTO, otherPostingsDTO, lastId, path, userMap.values(),
                learning, postDTO, nodeCompleted, courseProgress, courseDO.getRootNodeId(), userId); // course 的 rootNodeId
    }

    /**
     * 核心页面数据聚合逻辑（公开版本，无需userId）
     * 用于匿名用户，所有个性化字段返回默认值
     */
    private Map<String, Object> readPageDataPublic(CourseDO courseDO, String path) {
        // 使用假的userId=0来获取TOC（不包含完成状态）
        Utils.Pair<String, Map<Long, NodeWithProgressDTO>> response = getTocPublic(courseDO.getId());
        NodeTocDTO nodeTocDTO = new NodeTocDTO(response.left(), response.right());

        JsonNode rootNode = parseJsonSafely(nodeTocDTO.getContents());
        path = sanitizePath(path, courseDO.getRootNodeId());

        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, courseDO.getRootNodeId());

        List<Long> userIds = new LinkedList<>();
        PostDO chosenPosting = extractChosenPosting(pair.right(), userIds);

        long nodeId = pair.left();
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);

        if (nodeDO.getState() != null && nodeDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.NODE_STATE_INVALID.exception();
        }

        List<PostDO> otherPostings = getOtherPostings(nodeDO.getId(), userIds);
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        // 转换帖子为 DTO 并填充关联信息（公开版本不需要投票信息）
        PostWithVoteDTO chosenPostingDTO = convertPostToDTO(chosenPosting, userMap, null);
        List<PostWithVoteDTO> otherPostingsDTO = convertPostListToDTO(otherPostings, userMap, null);

        long lastId = calculateLastId(chosenPosting, otherPostings);

        // 构建父课程（无个性化信息）
        CourseFullDTO parentCourse;
        if (courseDO.getParentCourseId() != 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            parentCourse = courseAssembler.toFullDTO(parentCourseDO, null);
        } else {
            parentCourse = courseAssembler.toFullDTO(courseDO, null);
        }

        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        return buildPageDataResponsePublic(nodeTocDTO, nodeDO, parentCourse, courseDO, subCourseList,
                chosenPostingDTO, otherPostingsDTO, lastId, path, userMap.values(), courseDO.getRootNodeId());
    }

    // ========== 私有辅助方法 ==========

    private CourseDO validateCourseExists(Long courseId) {
        CourseDO courseDO = courseDataService.validateAndGet(courseId);
        if (courseDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.COURSE_IS_NOT_PUBLISHED.exception();
        }
        return courseDO;
    }
    
    private void validatePath(String path) {
        if (!systemProperties.getPage().isEnablePathValidation()) {
            return;
        }
        
        if (path != null && !path.isEmpty() && !PATH_PATTERN.matcher(path).matches()) {
            log.warn("无效的路径格式: {}", path);
            // 可以选择抛异常或者记录日志
        }
    }
    
    private String generateDefaultPath(Long rootNode) {
        return DEFAULT_PATH_PREFIX + rootNode;
    }
    
    private JsonNode parseJsonSafely(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败: {}", json, e);
            throw StatusCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }
    
    private String sanitizePath(String path, Long rootNode) {
        if (path == null || path.isEmpty() || !path.contains(PATH_SEPARATOR)) {
            return generateDefaultPath(rootNode);
        }
        return path;
    }
    
    private Utils.Pair<Long, JsonNode> getNodeByPath(JsonNode rootNode, String path, Long rootNode2) {
        Utils.Pair<Long, JsonNode> pair = Utils.getNodeByPath(rootNode, path);
        
        if (pair == null && systemProperties.getPage().isEnableAutoPathRepair()) {
            log.warn("路径{}不存在，自动修复为默认路径", path);
            String defaultPath = generateDefaultPath(rootNode2);
            pair = Utils.getNodeByPath(rootNode, defaultPath);
        }
        
        return pair;
    }
    
    private PostDO extractChosenPosting(JsonNode currentNode, List<Long> userIds) {
        if (currentNode != null && currentNode.has(CHOSEN_POST_FIELD)) {
            long postId = currentNode.get(CHOSEN_POST_FIELD).asInt();
            PostDO postDO = postDataService.getById(postId);
            if (postDO != null) {
                userIds.add(postDO.getCreatorId());
                return postDO;
            }
        }
        return null;
    }


    private List<PostDO> getOtherPostings(Long nodeId, List<Long> userIds) {
        List<PostDO> postDOList = postService.getList(nodeId);
        postDOList.forEach(postDO -> userIds.add(postDO.getCreatorId()));
        return postDOList;
    }
    
    private Map<Long, UserBriefDTO> buildUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }

        List<UserBriefDTO> userList = userConverter.toBriefDTO(userDataService.getByIds(userIds));
        return userList.stream().collect(Collectors.toMap(UserBriefDTO::getId, user -> user));
    }

    /**
     * 将单个 PostDO 转换为 PostWithVoteDTO 并填充关联信息
     */
    private PostWithVoteDTO convertPostToDTO(PostDO postDO, Map<Long, UserBriefDTO> userMap, Long userId) {
        if (postDO == null) {
            return null;
        }

        PostWithVoteDTO dto = postConverter.toWithVoteDTO(postDO);
        dto.setCreator(userMap.get(dto.getCreatorId()));

        if (userId != null) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, List.of(postDO.getId()), ContentType.post.value());
            if (!upvotes.isEmpty()) {
                dto.setVoteType(upvotes.get(0).getType());
            }
        }

        // 填充统计数据（累计 + 今日 Redis 增量）
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.post, postDO.getId());
        dto.setViewCount(stats.getViewCount());
        dto.setTwiceCount(stats.getTwiceCount());
        dto.setLikeCount(stats.getLikeCount());
        dto.setCommentCount(stats.getCommentCount());

        return dto;
    }

    /**
     * 将 PostDO 列表转换为 PostWithVoteDTO 列表并填充关联信息
     */
    private List<PostWithVoteDTO> convertPostListToDTO(List<PostDO> postDOList, Map<Long, UserBriefDTO> userMap, Long userId) {
        if (postDOList == null || postDOList.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostWithVoteDTO> dtoList = postConverter.toWithVoteDTO(postDOList);
        List<Long> postIds = postDOList.stream().map(PostDO::getId).collect(Collectors.toList());

        // 填充创建者信息
        dtoList.forEach(dto -> dto.setCreator(userMap.get(dto.getCreatorId())));

        // 填充投票信息
        if (userId != null) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, ContentType.post.value());
            Map<Long, Integer> voteTypes = upvotes.stream()
                    .collect(Collectors.toMap(UpvoteDO::getObjectId, UpvoteDO::getType));

            dtoList.forEach(dto -> {
                if (voteTypes.containsKey(dto.getId())) {
                    dto.setVoteType(voteTypes.get(dto.getId()));
                }
            });
        }

        // 批量填充统计数据（累计 + 今日 Redis 增量）
        Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(ContentType.post, postIds);
        dtoList.forEach(dto -> {
            ContentStatsDTO stats = statsMap.get(dto.getId());
            if (stats != null) {
                dto.setViewCount(stats.getViewCount());
                dto.setTwiceCount(stats.getTwiceCount());
                dto.setLikeCount(stats.getLikeCount());
                dto.setCommentCount(stats.getCommentCount());
            }
        });

        return dtoList;
    }
    
    private PostWithVoteDTO buildPostDTO(PostDO postDO, Map<Long, UserBriefDTO> userMap, Long userId) {
        if (postDO == null) {
            return null;
        }

        PostWithVoteDTO postDTO = postConverter.toWithVoteDTO(postDO);
        postDTO.setCreator(userMap.get(postDTO.getCreatorId()));

        // 目录类型帖子：将逗号分隔的 node id 转换为包含完整 node 信息的 JSON
        if (postDO.getType() != null && postDO.getType() == PostType.index.value()) {
            String enrichedContent = enrichIndexPostContent(postDO.getContent());
            postDTO.setContent(enrichedContent);
        }

        if (userId != null) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, List.of(postDO.getId()), ContentType.post.value());
            if (!upvotes.isEmpty()) {
                postDTO.setVoteType(upvotes.get(0).getType());
            }
        }

        // 填充统计数据（累计 + 今日 Redis 增量）
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(ContentType.post, postDO.getId());
        postDTO.setViewCount(stats.getViewCount());
        postDTO.setTwiceCount(stats.getTwiceCount());
        postDTO.setLikeCount(stats.getLikeCount());
        postDTO.setCommentCount(stats.getCommentCount());

        return postDTO;
    }

    /**
     * 将目录类型帖子的 content（逗号分隔的 node id）转换为包含完整 node 信息的 JSON
     * 输入格式：1,2,3
     * 输出格式：[{"id": 1, "name": "节点名", "description": "描述"}, ...]
     */
    private String enrichIndexPostContent(String content) {
        if (content == null || content.isBlank()) {
            return "[]";
        }

        try {
            // 解析逗号分隔的 node id
            List<Long> nodeIds = Arrays.stream(content.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            if (nodeIds.isEmpty()) {
                return "[]";
            }

            // 批量查询 node 信息
            Map<Long, NodeDO> nodeMap = nodeDataService.getMapByIds(nodeIds);

            // 构建结果列表，保持原顺序
            List<NodeSimpleDTO> nodeList = new ArrayList<>();
            for (Long nodeId : nodeIds) {
                NodeDO nodeDO = nodeMap.get(nodeId);
                NodeSimpleDTO dto = new NodeSimpleDTO();
                dto.setId(nodeId);
                if (nodeDO != null) {
                    dto.setName(nodeDO.getName());
                    dto.setDescription(nodeDO.getDescription());
                } else {
                    dto.setName("节点 #" + nodeId);
                    dto.setDescription("");
                }
                nodeList.add(dto);
            }

            return objectMapper.writeValueAsString(nodeList);
        } catch (Exception e) {
            log.warn("页面服务 目录帖子内容解析失败: {}", content, e);
            return "[]";
        }
    }

    private long calculateLastId(PostDO chosenPosting, List<PostDO> otherPostings) {
        long lastId = -1;

        if (chosenPosting != null) lastId = Math.max(lastId, chosenPosting.getId());
        if (otherPostings != null) {
            lastId = Math.max(lastId, otherPostings.stream().mapToLong(PostDO::getId).max().orElse(-1));
        }

        return lastId;
    }

    private boolean checkLearningStatus(long userId, Long courseId) {
        return userLearningService.isLearningCourse(userId, courseId);
    }

    private CourseFullDTO buildParentCourse(CourseDO courseDO, long userId) {
        if (courseDO.getParentCourseId() != 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            return courseAssembler.toFullDTO(parentCourseDO, userId);
        } else {
            return courseAssembler.toFullDTO(courseDO, userId);
        }
    }

    private Map<String, Object> buildPageDataResponse(NodeTocDTO nodeTocDTO, NodeDO nodeDO,
                                                      CourseFullDTO parentCourse, CourseDO courseDO, List<CourseSummaryDTO> subCourseList,
                                                      PostWithVoteDTO chosenPosting, List<PostWithVoteDTO> otherPostings,
                                                      long lastId, String path, Collection<UserBriefDTO> users, boolean learning,
                                                      PostWithVoteDTO postDTO, boolean nodeCompleted, Integer courseProgress, Long rootNodeId, Long userId) {

        Map<String, Object> data = new HashMap<>();

        try {
            List<Object> contents = objectMapper.readValue(nodeTocDTO.getContents(), List.class);
            data.put("toc", contents);
        } catch (JsonProcessingException e) {
            log.error("TOC内容解析失败", e);
            data.put("toc", new ArrayList<>());
        }

        // 填充 node 统计数据
        NodeWithProgressDTO nodeDTO = nodeConverter.toWithProgressDTO(nodeDO, nodeCompleted);
        ContentStatsDTO nodeStats = contentStatsDomainService.getContentStats(ContentType.node, nodeDO.getId());
        nodeDTO.setCommentCount(nodeStats.getCommentCount());
        nodeDTO.setNodeReferenceCount(nodeStats.getNodeReferenceCount());

        data.put("node", nodeDTO);
        data.put("parentCourse", parentCourse);
        data.put("course", courseAssembler.toFullDTO(courseDO, userId));
        data.put("subCourseList", subCourseList);
        data.put("chosenPosting", chosenPosting);
        data.put("otherPostings", otherPostings);
        data.put("lastId", lastId);
        data.put("tocNodeInfos", nodeTocDTO.getNodeInfos());
        data.put("path", path);
        data.put("users", new ArrayList<>(users));
        data.put("learning", learning);
        data.put("rootNodeId", rootNodeId); // TOC 是基于此 nodeId 创建的

        // 如果正在学习，计算可完成的节点列表
        if (learning && rootNodeId != null && userId != null) {
            List<Long> completableNodeIds = learningProgressService.findCompletableNodes(userId, rootNodeId);
            data.put("completableNodeIds", completableNodeIds);
        } else {
            data.put("completableNodeIds", new ArrayList<>());
        }

        if (postDTO != null) {
            data.put("post", postDTO);
        }

        return data;
    }

    /**
     * 构建页面数据响应（公开版本）
     * 不包含个性化字段：learning=false, nodeCompleted=false, courseProgress=0
     */
    private Map<String, Object> buildPageDataResponsePublic(NodeTocDTO nodeTocDTO, NodeDO nodeDO,
                                                            CourseFullDTO parentCourse, CourseDO courseDO, List<CourseSummaryDTO> subCourseList,
                                                            PostWithVoteDTO chosenPosting, List<PostWithVoteDTO> otherPostings,
                                                            long lastId, String path, Collection<UserBriefDTO> users, Long rootNodeId) {

        Map<String, Object> data = new HashMap<>();

        try {
            List<Object> contents = objectMapper.readValue(nodeTocDTO.getContents(), List.class);
            data.put("toc", contents);
        } catch (JsonProcessingException e) {
            log.error("TOC内容解析失败", e);
            data.put("toc", new ArrayList<>());
        }

        // 填充 node 统计数据
        NodeWithProgressDTO nodeDTO = nodeConverter.toWithProgressDTO(nodeDO, false);
        ContentStatsDTO nodeStats = contentStatsDomainService.getContentStats(ContentType.node, nodeDO.getId());
        nodeDTO.setCommentCount(nodeStats.getCommentCount());

        data.put("node", nodeDTO);
        data.put("parentCourse", parentCourse);
        data.put("course", courseAssembler.toFullDTO(courseDO, null)); // 未登录用户
        data.put("subCourseList", subCourseList);
        data.put("chosenPosting", chosenPosting);
        data.put("otherPostings", otherPostings);
        data.put("lastId", lastId);
        data.put("tocNodeInfos", nodeTocDTO.getNodeInfos());
        data.put("path", path);
        data.put("users", new ArrayList<>(users));
        data.put("learning", false); // 未学习
        data.put("rootNodeId", rootNodeId); // TOC 是基于此 nodeId 创建的

        return data;
    }
}
