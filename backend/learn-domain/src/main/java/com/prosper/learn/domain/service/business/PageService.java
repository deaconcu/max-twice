package com.prosper.learn.domain.service.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.ContentsService;
import com.prosper.learn.domain.util.converter.NodeConverter;
import com.prosper.learn.domain.util.converter.PostConverter;
import com.prosper.learn.domain.util.converter.UserConverter;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.prosper.learn.common.Enums.ObjectType.post;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageService {

    private final ContentsService contentsService;
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

    public Utils.Pair<String, Map<Long, NodeDTO>> getToc(long userId, long courseId, boolean create) {

        ArrayNode arrayNode = contentsService.getToc(userId, courseId, create);

        Set<Long> keys = new HashSet<>();
        Utils.collectKeys(arrayNode, keys);

        List<NodeDO> nodeList = keys.isEmpty() ? new ArrayList<>() : nodeDataService.getByIds(keys.stream().toList());

        // 获取用户完成的节点集合
        Set<Integer> completedNodes = learningProgressService.getUserCompletedNodes(userId);

        // 构建包含完成状态的节点信息
        Map<Long, NodeDTO> nodeInfos = nodeList.stream()
                .collect(Collectors.toMap(
                        NodeDO::getId,
                        node -> nodeConverter.toDTOV2(node, completedNodes.contains(node.getId()))
                ));


        return new Utils.Pair<>(arrayNode.toString(), nodeInfos);
    }

    /**
     * 根据评论ID读取页面数据
     */
    public Map<String, Object> readPageByComment(Long commentId, long userId) {
        validateCommentId(commentId);
        validateUserId(userId);
        
        CommentDO commentDO = validateCommentExists(commentId);

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

        if (commentDO.getObjectType() == post.value()) {
            long postId = commentDO.getObjectId();
            postDO = postService.validateAndGetPost(postId);
            nodeDO = validateNodeExists(postDO.getNodeId());
            courseDO = validateCourseExists(nodeDO.getCourseId());
            path = generateDefaultPath(courseDO.getRootNodeId());
        } else {
            nodeDO = validateNodeExists(commentDO.getObjectId());
            courseDO = validateCourseExists(nodeDO.getCourseId());
            path = generateDefaultPath(courseDO.getRootNodeId());
        }

        return readPageData(courseDO, path, nodeDO, postDO, userId);
    }

    /**
     * 根据帖子ID读取页面数据
     */
    public Map<String, Object> readPageByPost(Long postId, long userId) {
        validatePostId(postId);
        validateUserId(userId);

        PostDO postDO = postService.validateAndGetPost(postId);
        NodeDO nodeDO = validateNodeExists(postDO.getNodeId());
        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
        String path = generateDefaultPath(courseDO.getRootNodeId());

        return readPageData(courseDO, path, nodeDO, postDO, userId);
    }

    /**
     * 根据节点ID读取页面数据
     */
    public Map<String, Object> readPageByNode(Long nodeId, long userId) {
        validateNodeId(nodeId);
        validateUserId(userId);
        
        NodeDO nodeDO = validateNodeExists(nodeId);
        CourseDO courseDO = validateCourseExists(nodeDO.getCourseId());
        String path = generateDefaultPath(courseDO.getRootNodeId());

        return readPageData(courseDO, path, nodeDO, null, userId);
    }

    /**
     * 根据课程ID和路径读取页面数据
     */
    public Map<String, Object> readPageByPath(Long courseId, String path, long userId) {
        validateCourseId(courseId);
        validateUserId(userId);
        validatePath(path);
        
        CourseDO courseDO = validateCourseExists(courseId);
        return readPageData(courseDO, path, null, null, userId);
    }

    /**
     * 核心页面数据聚合逻辑
     */
    private Map<String, Object> readPageData(CourseDO courseDO, String path, NodeDO nodeDO, PostDO postDO, long userId) {
        Utils.Pair<String, Map<Long, NodeDTO>> response = getToc(userId, courseDO.getId(), true);
        CourseTocDTO courseTocDTO = new CourseTocDTO(response.left(), response.right());

        JsonNode rootNode = parseJsonSafely(courseTocDTO.getContents());
        path = sanitizePath(path, courseDO.getRootNodeId());
        
        Utils.Pair<Long, JsonNode> pair = getNodeByPath(rootNode, path, courseDO.getRootNodeId());
        
        List<Long> userIds = new LinkedList<>();
        PostDTO chosenPosting = extractChosenPosting(pair.right(), userIds);
        List<PostDTO> fixedPostings = extractFixedPostings(pair.right(), userIds);
        
        if (nodeDO == null) {
            long nodeId = pair.left();
            nodeDO = validateNodeExists(nodeId);
        }
        
        List<PostDTO> otherPostings = getOtherPostings(nodeDO.getId(), userIds);
        Map<Long, UserDTO> userMap = buildUserMap(userIds);
        
        setPostCreators(chosenPosting, fixedPostings, otherPostings, userMap);
        setVoteTypes(userId, chosenPosting, fixedPostings, otherPostings);
        
        PostDTO postDTO = buildPostDTO(postDO, userMap);
        long lastId = calculateLastId(chosenPosting, fixedPostings, otherPostings);
        
        boolean learning = checkLearningStatus(userId, courseDO.getId());
        CourseDTO parentCourse = buildParentCourse(courseDO, userId);
        List<CourseDTO> subCourseList = courseService.getSubCourses(parentCourse.getId());
        
        boolean nodeCompleted = learningProgressService.isNodeCompleted(userId, nodeDO.getId());
        Integer courseProgress = userCourseService.getCourseProgress(userId, courseDO.getId());
        
        return buildPageDataResponse(courseTocDTO, nodeDO, parentCourse, courseDO, subCourseList,
                chosenPosting, fixedPostings, otherPostings, lastId, path, userMap.values(),
                learning, postDTO, nodeCompleted, courseProgress);
    }

    // ========== 私有辅助方法 ==========
    
    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateCommentId(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateNodeId(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private void validateCourseId(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    private CommentDO validateCommentExists(Long commentId) {
        CommentDO commentDO = commentDataService.getById(commentId);
        if (commentDO == null) {
            throw ErrorCode.COMMENT_NOT_FOUND.exception();
        }
        return commentDO;
    }
    
    private NodeDO validateNodeExists(Long nodeId) {
        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO == null) {
            throw ErrorCode.POSTING_NODE_NOT_FOUND.exception();
        }
        return nodeDO;
    }
    
    private CourseDO validateCourseExists(Long courseId) {
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.COURSE_NOT_FOUND.exception();
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
    
    private PostDTO extractChosenPosting(JsonNode currentNode, List<Long> userIds) {
        if (currentNode != null && currentNode.has(CHOSEN_POST_FIELD)) {
            PostDTO chosenPostDO = postService.getDTO(currentNode.get(CHOSEN_POST_FIELD).asInt());
            if (chosenPostDO != null) {
                userIds.add(chosenPostDO.getCreatorId());
                return chosenPostDO;
            }
        }
        return null;
    }
    
    private List<PostDTO> extractFixedPostings(JsonNode currentNode, List<Long> userIds) {
        if (currentNode != null && currentNode.has(FIXED_POSTS_FIELD)) {
            ArrayNode idsNode = (ArrayNode) currentNode.get(FIXED_POSTS_FIELD);
            List<Long> fixedIds = objectMapper.convertValue(idsNode, List.class);
            
            List<PostDTO> postDOList = postService.getDTOList(fixedIds);
            postDOList.forEach(postDO -> userIds.add(postDO.getCreatorId()));
            
            return postDOList;
        }
        return null;
    }
    
    private List<PostDTO> getOtherPostings(Long nodeId, List<Long> userIds) {
        List<PostDTO> postDTOList = postService.getList(nodeId);
        postDTOList.forEach(postDO -> userIds.add(postDO.getCreatorId()));
        return postDTOList;
    }
    
    private Map<Long, UserDTO> buildUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<UserDTO> userList = userConverter.toDTOV1(userDataService.getByIds(userIds));
        return userList.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));
    }
    
    private void setPostCreators(PostDTO chosenPosting, List<PostDTO> fixedPostings, List<PostDTO> otherPostings, Map<Long, UserDTO> userMap) {
        if (chosenPosting != null) {
            chosenPosting.setCreator(userMap.get(chosenPosting.getCreatorId()));
        }
        
        if (fixedPostings != null) {
            fixedPostings.forEach(post -> post.setCreator(userMap.get(post.getCreatorId())));
        }
        
        if (otherPostings != null) {
            otherPostings.forEach(post -> post.setCreator(userMap.get(post.getCreatorId())));
        }
    }
    
    private void setVoteTypes(long userId, PostDTO chosenPosting, List<PostDTO> fixedPostings, List<PostDTO> otherPostings) {
        List<Long> allPostingIds = new ArrayList<>();
        
        if (chosenPosting != null) allPostingIds.add(chosenPosting.getId());
        if (fixedPostings != null) fixedPostings.forEach(post -> allPostingIds.add(post.getId()));
        if (otherPostings != null) otherPostings.forEach(post -> allPostingIds.add(post.getId()));
        
        if (allPostingIds.isEmpty()) {
            return;
        }
        
        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, post.value());
        Map<Long, Integer> voteTypes = upvotes.stream()
            .collect(Collectors.toMap(UpvoteDO::getObjectId, UpvoteDO::getType));
        
        if (chosenPosting != null && voteTypes.containsKey(chosenPosting.getId())) {
            chosenPosting.setVoteType(voteTypes.get(chosenPosting.getId()));
        }
        
        if (fixedPostings != null) {
            fixedPostings.forEach(post -> {
                if (voteTypes.containsKey(post.getId())) {
                    post.setVoteType(voteTypes.get(post.getId()));
                }
            });
        }
        
        if (otherPostings != null) {
            otherPostings.forEach(post -> {
                if (voteTypes.containsKey(post.getId())) {
                    post.setVoteType(voteTypes.get(post.getId()));
                }
            });
        }
    }
    
    private PostDTO buildPostDTO(PostDO postDO, Map<Long, UserDTO> userMap) {
        if (postDO == null) {
            return null;
        }
        
        PostDTO postDTO = postConverter.toDTO(postDO);
        postDTO.setCreator(userMap.get(postDTO.getCreatorId()));
        return postDTO;
    }
    
    private long calculateLastId(PostDTO chosenPosting, List<PostDTO> fixedPostings, List<PostDTO> otherPostings) {
        long lastId = -1;
        
        if (chosenPosting != null) lastId = Math.max(lastId, chosenPosting.getId());
        if (fixedPostings != null) {
            lastId = Math.max(lastId, fixedPostings.stream().mapToLong(PostDTO::getId).max().orElse(-1));
        }
        if (otherPostings != null) {
            lastId = Math.max(lastId, otherPostings.stream().mapToLong(PostDTO::getId).max().orElse(-1));
        }
        
        return lastId;
    }
    
    private boolean checkLearningStatus(long userId, Long courseId) {
        UserCourseDO userCourseDo = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return userCourseDo != null;
    }
    
    private CourseDTO buildParentCourse(CourseDO courseDO, long userId) {
        CourseDTO parentCourse;
        if (courseDO.getParentCourseId() != 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            parentCourse = courseService.toDTOV4(parentCourseDO);
        } else {
            parentCourse = courseService.toDTOV4(courseDO);
        }
        
        boolean subscribed = checkSubscriptionStatus(userId, parentCourse.getId());
        parentCourse.setSubscribed(subscribed);
        
        return parentCourse;
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
                                                      CourseDTO parentCourse, CourseDO courseDO, List<CourseDTO> subCourseList,
                                                      PostDTO chosenPosting, List<PostDTO> fixedPostings, List<PostDTO> otherPostings,
                                                      long lastId, String path, Collection<UserDTO> users, boolean learning,
                                                      PostDTO postDTO, boolean nodeCompleted, Integer courseProgress) {
        
        Map<String, Object> data = new HashMap<>();
        
        try {
            List<Object> contents = objectMapper.readValue(courseTocDTO.getContents(), List.class);
            data.put("toc", contents);
        } catch (JsonProcessingException e) {
            log.error("TOC内容解析失败", e);
            data.put("toc", new ArrayList<>());
        }

        data.put("node", nodeConverter.toDTOV2(nodeDO, nodeCompleted));
        data.put("parentCourse", parentCourse);
        data.put("course", courseService.toDTOV5(courseDO, parentCourse.getSubscribed(), courseProgress));
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
}
