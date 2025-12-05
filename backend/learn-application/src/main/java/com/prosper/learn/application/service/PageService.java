package com.prosper.learn.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.business.service.domain.TocDomainService;
import com.prosper.learn.business.util.converter.NodeConverter;
import com.prosper.learn.business.util.converter.PostConverter;
import com.prosper.learn.business.util.converter.UserConverter;
import com.prosper.learn.dto.response.node.NodeWithProgressDTO;
import com.prosper.learn.dto.response.post.PostWithVoteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.ContentType.post;

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
    private final UserCourseDataService userCourseDataService;
    private final UserProfileDataService userProfileDataService;
    private final SystemProperties systemProperties;
    private final NodeConverter nodeConverter;
    private final PostConverter postConverter;
    private final UserConverter userConverter;
    private final CourseService courseService;
    private final UserCourseService userCourseService;

    
    // ========== 常量定义 ==========
    
    private static final String DEFAULT_PATH_PREFIX = "1-";
    private static final String PATH_SEPARATOR = "-";
    private static final String FIXED_POSTS_FIELD = "^";
    private static final String CHOSEN_POST_FIELD = "+";
    private static final Pattern PATH_PATTERN = Pattern.compile("^\\d+(\\-\\d+)*$");

    
    // ========== 公共方法 ==========

    public Utils.Pair<String, Map<Long, NodeWithProgressDTO>> getToc(long userId, long courseId, boolean create) {

        ArrayNode arrayNode = tocService.getToc(userId, courseId, create);

        Set<Long> keys = new HashSet<>();
        Utils.collectKeys(arrayNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeDataService.getByIds(keys.stream().toList());

        // 获取用户完成的节点集合
        Set<Long> completedNodes = learningProgressService.getUserCompletedNodes(userId);

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
        ArrayNode arrayNode = tocService.getToc(0L, courseId, true);

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
        NodeDO nodeDO = nodeDataService.validateAndGet(postDO.getNodeId());
        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
        String path = generateDefaultPath(courseDO.getRootNodeId());

        return readPageData(courseDO, path, nodeDO, postDO, userId);
    }

    /**
     * 根据节点ID读取页面数据
     */
    public Map<String, Object> readPageByNode(Long nodeId, long userId) {
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);
        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
        String path = generateDefaultPath(courseDO.getRootNodeId());

        return readPageData(courseDO, path, nodeDO, null, userId);
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
        Utils.Pair<String, Map<Long, NodeWithProgressDTO>> response = getToc(userId, courseDO.getId(), true);
        CourseTocDTO courseTocDTO = new CourseTocDTO(response.left(), response.right());

        JsonNode rootNode = parseJsonSafely(courseTocDTO.getContents());
        path = sanitizePath(path, courseDO.getRootNodeId());

        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, courseDO.getRootNodeId());

        List<Long> userIds = new LinkedList<>();
        PostDO chosenPosting = extractChosenPosting(pair.right(), userIds);
        List<PostDO> fixedPostings = extractFixedPostings(pair.right(), userIds);

        if (nodeDO == null) {
            long nodeId = pair.left();
            nodeDO = nodeDataService.validateAndGet(nodeId);
        }

        if (nodeDO.getState() != null && nodeDO.getState() != Enums.ContentState.PUBLISHED.value()) {
            throw ErrorCode.NODE_STATE_INVALID.exception();
        }

        List<PostDO> otherPostings = getOtherPostings(nodeDO.getId(), userIds);
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        PostWithVoteDTO postDTO = buildPostDTO(postDO, userMap, userId);

        // 转换帖子为 DTO 并填充关联信息
        PostWithVoteDTO chosenPostingDTO = convertPostToDTO(chosenPosting, userMap, userId);
        List<PostWithVoteDTO> fixedPostingsDTO = convertPostListToDTO(fixedPostings, userMap, userId);
        List<PostWithVoteDTO> otherPostingsDTO = convertPostListToDTO(otherPostings, userMap, userId);

        long lastId = calculateLastId(chosenPosting, fixedPostings, otherPostings);

        boolean learning = checkLearningStatus(userId, courseDO.getId());
        CourseWithProgressDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());
        Integer courseProgress = userCourseService.getCourseProgress(userId, courseDO.getId());

        return buildPageDataResponse(courseTocDTO, nodeDO, parentCourse, courseDO, subCourseList,
                chosenPostingDTO, fixedPostingsDTO, otherPostingsDTO, lastId, path, userMap.values(),
                learning, postDTO, nodeCompleted, courseProgress);
    }

    /**
     * 核心页面数据聚合逻辑（公开版本，无需userId）
     * 用于匿名用户，所有个性化字段返回默认值
     */
    private Map<String, Object> readPageDataPublic(CourseDO courseDO, String path) {
        // 使用假的userId=0来获取TOC（不包含完成状态）
        Utils.Pair<String, Map<Long, NodeWithProgressDTO>> response = getTocPublic(courseDO.getId());
        CourseTocDTO courseTocDTO = new CourseTocDTO(response.left(), response.right());

        JsonNode rootNode = parseJsonSafely(courseTocDTO.getContents());
        path = sanitizePath(path, courseDO.getRootNodeId());

        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, courseDO.getRootNodeId());

        List<Long> userIds = new LinkedList<>();
        PostDO chosenPosting = extractChosenPosting(pair.right(), userIds);
        List<PostDO> fixedPostings = extractFixedPostings(pair.right(), userIds);

        long nodeId = pair.left();
        NodeDO nodeDO = nodeDataService.validateAndGet(nodeId);

        if (nodeDO.getState() != null && nodeDO.getState() != Enums.ContentState.PUBLISHED.value()) {
            throw ErrorCode.NODE_STATE_INVALID.exception();
        }

        List<PostDO> otherPostings = getOtherPostings(nodeDO.getId(), userIds);
        Map<Long, UserBriefDTO> userMap = buildUserMap(userIds);

        // 转换帖子为 DTO 并填充关联信息（公开版本不需要投票信息）
        PostWithVoteDTO chosenPostingDTO = convertPostToDTO(chosenPosting, userMap, null);
        List<PostWithVoteDTO> fixedPostingsDTO = convertPostListToDTO(fixedPostings, userMap, null);
        List<PostWithVoteDTO> otherPostingsDTO = convertPostListToDTO(otherPostings, userMap, null);

        long lastId = calculateLastId(chosenPosting, fixedPostings, otherPostings);

        // 构建父课程（无个性化信息）
        CourseWithProgressDTO parentCourse;
        if (courseDO.getParentCourseId() != 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            parentCourse = courseService.toWithProgressDTO(parentCourseDO, false, 0);
        } else {
            parentCourse = courseService.toWithProgressDTO(courseDO, false, 0);
        }

        List<CourseSummaryDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());

        return buildPageDataResponsePublic(courseTocDTO, nodeDO, parentCourse, courseDO, subCourseList,
                chosenPostingDTO, fixedPostingsDTO, otherPostingsDTO, lastId, path, userMap.values());
    }

    // ========== 私有辅助方法 ==========

    private CourseDO validateCourseExists(Long courseId) {
        CourseDO courseDO = courseDataService.validateAndGet(courseId);
        if (courseDO.getState() != Enums.ContentState.PUBLISHED.value()) {
            throw ErrorCode.COURSE_IS_NOT_PUBLISHED.exception();
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
            throw ErrorCode.JSON_PROCESSING_ERROR.exception(e);
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

    private List<PostDO> extractFixedPostings(JsonNode currentNode, List<Long> userIds) {
        if (currentNode != null && currentNode.has(FIXED_POSTS_FIELD)) {
            ArrayNode idsNode = (ArrayNode) currentNode.get(FIXED_POSTS_FIELD);
            List<Long> fixedIds = objectMapper.convertValue(idsNode, List.class);
            
            List<PostDO> postDOList = postDataService.getByIds(fixedIds);
            postDOList.forEach(postDO -> userIds.add(postDO.getCreatorId()));
            
            return postDOList;
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

        // 填充创建者信息
        dtoList.forEach(dto -> dto.setCreator(userMap.get(dto.getCreatorId())));

        // 填充投票信息
        if (userId != null) {
            List<Long> postIds = postDOList.stream().map(PostDO::getId).collect(Collectors.toList());
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, ContentType.post.value());
            Map<Long, Integer> voteTypes = upvotes.stream()
                    .collect(Collectors.toMap(UpvoteDO::getObjectId, UpvoteDO::getType));

            dtoList.forEach(dto -> {
                if (voteTypes.containsKey(dto.getId())) {
                    dto.setVoteType(voteTypes.get(dto.getId()));
                }
            });
        }

        return dtoList;
    }
    
    private PostWithVoteDTO buildPostDTO(PostDO postDO, Map<Long, UserBriefDTO> userMap, Long userId) {
        if (postDO == null) {
            return null;
        }

        PostWithVoteDTO postDTO = postConverter.toWithVoteDTO(postDO);
        postDTO.setCreator(userMap.get(postDTO.getCreatorId()));

        if (userId != null) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, List.of(postDO.getId()), ContentType.post.value());
            if (!upvotes.isEmpty()) {
                postDTO.setVoteType(upvotes.get(0).getType());
            }
        }

        return postDTO;
    }

    private long calculateLastId(PostDO chosenPosting, List<PostDO> fixedPostings, List<PostDO> otherPostings) {
        long lastId = -1;

        if (chosenPosting != null) lastId = Math.max(lastId, chosenPosting.getId());
        if (fixedPostings != null) {
            lastId = Math.max(lastId, fixedPostings.stream().mapToLong(PostDO::getId).max().orElse(-1));
        }
        if (otherPostings != null) {
            lastId = Math.max(lastId, otherPostings.stream().mapToLong(PostDO::getId).max().orElse(-1));
        }

        return lastId;
    }
    
    private boolean checkLearningStatus(long userId, Long courseId) {
        UserCourseDO userCourseDo = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return userCourseDo != null;
    }
    
    private CourseWithProgressDTO buildParentCourse(CourseDO courseDO, long userId) {
        boolean subscribed = checkSubscriptionStatus(userId, courseDO.getParentCourseId() != 0 ? courseDO.getParentCourseId() : courseDO.getId());

        if (courseDO.getParentCourseId() != 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            return courseService.toWithProgressDTO(parentCourseDO, subscribed, 0);
        } else {
            return courseService.toWithProgressDTO(courseDO, subscribed, 0);
        }
    }
    
    private boolean checkSubscriptionStatus(long userId, long courseId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        if (userProfileDO == null || userProfileDO.getSubscription() == null || userProfileDO.getSubscription().trim().isEmpty()) {
            return false;
        }
        
        try {
            List<Integer> subscriptionIds = Arrays.stream(userProfileDO.getSubscription().split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            return subscriptionIds.contains(courseId);
        } catch (NumberFormatException e) {
            log.error("订阅数据解析失败: {}", userProfileDO.getSubscription(), e);
            return false;
        }
    }
    
    private Map<String, Object> buildPageDataResponse(CourseTocDTO courseTocDTO, NodeDO nodeDO,
                                                      CourseWithProgressDTO parentCourse, CourseDO courseDO, List<CourseSummaryDTO> subCourseList,
                                                      PostWithVoteDTO chosenPosting, List<PostWithVoteDTO> fixedPostings, List<PostWithVoteDTO> otherPostings,
                                                      long lastId, String path, Collection<UserBriefDTO> users, boolean learning,
                                                      PostWithVoteDTO postDTO, boolean nodeCompleted, Integer courseProgress) {

        Map<String, Object> data = new HashMap<>();

        try {
            List<Object> contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            data.put("toc", contents);
        } catch (JsonProcessingException e) {
            log.error("TOC内容解析失败", e);
            data.put("toc", new ArrayList<>());
        }

        data.put("node", nodeConverter.toWithProgressDTO(nodeDO, nodeCompleted));
        data.put("parentCourse", parentCourse);
        data.put("course", courseService.toWithProgressDTO(courseDO, parentCourse.getSubscribed(), courseProgress));
        data.put("subCourseList", subCourseList);
        data.put("chosenPosting", chosenPosting);
        data.put("fixedPostings", fixedPostings);
        data.put("otherPostings", otherPostings);
        data.put("lastId", lastId);
        data.put("tocNodeInfos", courseTocDTO.getNodeInfos());
        data.put("path", path);
        data.put("users", new ArrayList<>(users));
        data.put("learning", learning);

        if (postDTO != null) {
            data.put("post", postDTO);
        }

        return data;
    }

    /**
     * 构建页面数据响应（公开版本）
     * 不包含个性化字段：learning=false, nodeCompleted=false, courseProgress=0
     */
    private Map<String, Object> buildPageDataResponsePublic(CourseTocDTO courseTocDTO, NodeDO nodeDO,
                                                            CourseWithProgressDTO parentCourse, CourseDO courseDO, List<CourseSummaryDTO> subCourseList,
                                                            PostWithVoteDTO chosenPosting, List<PostWithVoteDTO> fixedPostings, List<PostWithVoteDTO> otherPostings,
                                                            long lastId, String path, Collection<UserBriefDTO> users) {

        Map<String, Object> data = new HashMap<>();

        try {
            List<Object> contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            data.put("toc", contents);
        } catch (JsonProcessingException e) {
            log.error("TOC内容解析失败", e);
            data.put("toc", new ArrayList<>());
        }

        data.put("node", nodeConverter.toWithProgressDTO(nodeDO, false)); // 未完成
        data.put("parentCourse", parentCourse);
        data.put("course", courseService.toWithProgressDTO(courseDO, false, 0)); // 未订阅，进度0
        data.put("subCourseList", subCourseList);
        data.put("chosenPosting", chosenPosting);
        data.put("fixedPostings", fixedPostings);
        data.put("otherPostings", otherPostings);
        data.put("lastId", lastId);
        data.put("tocNodeInfos", courseTocDTO.getNodeInfos());
        data.put("path", path);
        data.put("users", new ArrayList<>(users));
        data.put("learning", false); // 未学习

        return data;
    }
}
