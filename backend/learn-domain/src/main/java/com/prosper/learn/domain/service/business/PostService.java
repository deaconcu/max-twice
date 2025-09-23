package com.prosper.learn.domain.service.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.BusinessException;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;
import com.prosper.learn.domain.service.basic.DailyStatsService;
import com.prosper.learn.domain.util.Util;
import com.prosper.learn.domain.util.converter.CourseConverter;
import com.prosper.learn.domain.util.converter.NodeConverter;
import com.prosper.learn.domain.util.converter.PostConverter;
import com.prosper.learn.domain.util.converter.UserConverter;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.dto.request.UpdatePostRequest;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.PostDTO;
import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.persistence.dataobject.*;
import com.prosper.learn.domain.service.data.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子服务
 * 
 * 负责管理系统中的帖子功能，包括：
 * - 帖子的创建、更新、删除和查询
 * - 支持文章类型和内容类型帖子
 * - 帖子审核流程
 * - 帖子点赞状态和用户信息的关联查询
 * - 分页和排序查询
 * 
 * 核心功能：
 * - 帖子内容的ID到名称转换
 * - 批量数据关联查询优化
 * - 支持基于分数的排序和分页
 * - 集成阅读量统计
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final NodeDataService nodeDataService;
    private final PostDataService postDataService;
    private final CourseDataService courseDataService;
    private final UpvoteDataService upvoteDataService;
    private final UserDataService userDataService;
    private final DailyStatsService dailyStatsService;
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;
    private final UserConverter userConverter;
    private final NodeConverter nodeConverter;
    private final CourseConverter courseConverter;
    private final PostConverter postConverter;
    private final CourseService courseService;
    private final NodeService nodeService;
    private final UserService userService;

    // =========== 公共方法 DTO ==========

    PostDTO toDTO(PostDO postDO) {
        return postConverter.toDTO(postDO);
    }

    List<PostDTO> toDTO(List<PostDO> postDOList) {
        return postConverter.toDTO(postDOList);
    }

    /**
     * v1 = v0 + creator
     */
    PostDTO toDTOV1(PostDO postDO) {
        PostDTO postDTO = postConverter.toDTO(postDO);
        UserDTO userDTO = userService.getUser(postDO.getCreatorId(), Enums.DTOVersion.V2);
        postDTO.setCreator(userDTO);
        return postDTO;
    }

    List<PostDTO> toDTOV1(List<PostDO> postDOList) {
        return postConverter.toDTO(postDOList);
    }

    /**
     * v2 = v0 + node + creator + views + votetype
     */
    PostDTO toDTOV2(PostDO postDO) {
        return postConverter.toDTO(postDO);
    }

    List<PostDTO> toDTOV2(List<PostDO> postDOList, long userId) {
        List<PostDTO> postDTOList = postConverter.toDTO(postDOList);

        // 设置views字段
        dailyStatsService.setViewsForPosts(postDTOList);

        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        List<Long> allPostingIds = new LinkedList<>();
        if (postDOList != null) postDOList.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Long, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, Enums.ObjectType.post.value());
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(nodeConverter.toDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(courseService.getCourseDTOV4ById(node.getCourseId()));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(userConverter.toDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * v2 = v0 + creator + voteType
     */
    List<PostDTO> toDTOV3(List<PostDO> postDOList, long userId) {
        List<Long> allPostingIds = new ArrayList<>();
        List<Long> userIds = new LinkedList<>();
        postDOList.forEach(postingDO -> {
            idToName(postingDO);
            allPostingIds.add(postingDO.getId());
            userIds.add(postingDO.getCreatorId());
        });

        List<UserDTO> userList = userIds.size() == 0 ?
                new ArrayList<>() : userConverter.toDTOV1(userDataService.getByIds(userIds));
        Map<Long, UserDTO> userMap = new HashMap<>();
        for (UserDTO user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostDTO> postDTOList = toDTO(postDOList);
        postDTOList.stream().forEach(item -> {
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, Enums.ObjectType.post.value());
            Map<Long, Integer> types = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }

            for (PostDTO posting : postDTOList) {
                if (types.containsKey(posting.getId()))
                    posting.setVoteType(types.get(posting.getId()));
            }
        }

        return postDTOList;
    }

    // =========== 公共方法 query ==========

    /**
     * 获取单个帖子
     */
    public PostDO get(long id) {
        PostDO post = validateAndGetPost(id);
        idToName(post);
        return post;
    }

    /**
     * 获取单个帖子DTO
     */
    public PostDTO getDTO(long id) {
        PostDO post = validateAndGetPost(id);
        idToName(post);

        return postConverter.toDTO(post);
    }

    /**
     * 批量获取帖子列表
     */
    public List<PostDTO> getDTOList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostDO> postings = postDataService.getByIds(ids);
        postings.forEach(this::idToName);
        return postConverter.toDTO(postings);
    }

    /**
     * 获取节点下的帖子列表（按分数排序），获取前 N 条
     */
    public List<PostDTO> getList(long nodeId) {
        validateNodeId(nodeId);
        List<PostDO> posts = postDataService.getListByNodeAndScore(nodeId, systemProperties.getPosting().getDefaultNodePostCount(), Enums.PostState.approved.value());
        posts.forEach(this::idToName);
        return postConverter.toDTO(posts);
    }

    /**
     * 获取用户文章或内容，这个在用户中心显示，带上这个帖子的views，用户能看到自己文章的阅读量
     */
    public List<PostDTO> getUserPosts(Long userId, Long lastId, String type) {
        validateUserId(lastId);
        userService.validateUserExists(userId);

        if ("content".equals(type)) {
            return getUserContentsWithViews(userId, lastId);
        } else {
            return getUserArticleWithViews(userId, lastId);
        }
    }

    /**
     * 获取用户文章列表（包含阅读量）
     */
    public List<PostDTO> getUserArticleWithViews(long userId, long lastId) {
        int count = systemProperties.getPosting().getUserContentsPageSize();
        List<PostDO> postings = postDataService.getArticleListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        return toDTOV2(postings, userId);

        /*
        List<PostDTO> postDTOList = postConverter.toDTO(postings);

        // 设置views字段
        dailyStatsService.setViewsForPosts(postDTOList);

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(nodeConverter.toDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(courseService.getCourseDTOV4ById(node.getCourseId()));

            postDTO.setCreator(userConverter.toDTOV1(userMap.get(postDTO.getCreatorId())));
        }

        return postDTOList;
         */
    }

    /**
     * 获取用户目录列表（包含阅读量）
     */
    public List<PostDTO> getUserContentsWithViews(long userId, long lastId) {
        int count = systemProperties.getPosting().getUserContentsPageSize();
        List<PostDO> postings = postDataService.getContentsListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        // 使用提取的公共方法处理内容ID转名称
        processContentPostsIdToName(postings);
        return toDTOV2(postings, userId);

        /*
        // 设置views字段
        dailyStatsService.setViewsForPosts(postDTOList);

        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        List<Long> allPostingIds = new LinkedList<>();
        if (postings != null) postings.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Long, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, Enums.ObjectType.post.value());
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(nodeConverter.toDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(courseService.getCourseDTOV4ById(node.getCourseId()));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(userConverter.toDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
        */
    }


    /**
     * 批量获取帖子（带用户信息和投票状态）
     */
    public List<PostDTO> getPostsWithUserAndVoteInfo(
            List<Long> ids, Long nodeId, double lastScore, Long lastPostingId, long userId) {
        List<PostDO> postDOList = null;
        if (ids != null && ids.size() > 0) {
            postDOList = postDataService.getByIds(ids);
        } else if (nodeId != null && nodeId > 0) {
            int count = 2;
            postDOList = postDataService.getListByNodeAndScoreAndPaginated(
                    nodeId, lastScore, lastPostingId, count, Enums.PostState.approved.value());
        }
        return toDTOV3(postDOList, userId);
    }

    /**
     * 获取节点帖子列表
     */
    public List<PostDTO> getNodePostsList(Long nodeId) {
        int count = systemProperties.getPosting().getDefaultNodeListCount();
        List<PostDO> postings = postDataService.getListByNode(nodeId, count, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return toDTO(postings);
    }

    /**
     * 获取待审核帖子列表
     */
    public List<PostDTO> getPendingPostsList() {
        List<PostDO> postDOList = postDataService.getListByState(Enums.PostState.approved.value(), systemProperties.getPosting().getPendingPostsLimit());
        for (PostDO postDO : postDOList) {
            if (postDO.getType() == Enums.PostType.contents.value()) {
                idToName(postDO);
            }
        }
        return toDTO(postDOList);
    }



    // ========== 公共方法 command ==========

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     *
     * @param request 帖子创建请求对象
     * @throws BusinessException 当节点不存在或JSON处理失败时抛出异常
     */
    @Transactional
    public void createPost(long userId, CreatePostRequest request) {
        createPost(userId, request, Enums.PostState.submited);
    }

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     *
     * @param request 帖子创建请求对象
     * @throws BusinessException 当节点不存在或JSON处理失败时抛出异常
     */
    @Transactional
    public void createPost(long userId, CreatePostRequest request, Enums.PostState postState) {
        // 先验证参数
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子对象不能为空");
        }
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效");
        }

        // 验证节点
        validateNodeId(request.getNodeId());

        if (request.getType() == Enums.PostType.contents.value()) {
            NodeDO nodeDO = nodeDataService.getById(request.getNodeId());
            if (nodeDO == null) {
                throw ErrorCode.POSTING_NODE_NOT_FOUND.exception();
            }

            List<String> nodeNames = parseJsonToStringList(request.getContent());
            String[] ids = new String[nodeNames.size()];

            // TODO 需要检查node name是否已存在，避免重复创建
            for (int i = 0; i < nodeNames.size(); i++) {
                NodeDO node = new NodeDO();
                node.setName(nodeNames.get(i));
                node.setDescription("");
                node.setCourseId(nodeDO.getCourseId());
                node.setCreatedAt(Utils.getLocalDateTime());
                node.setUpdatedAt(Utils.getLocalDateTime());
                node.setCreatorId(userId);
                nodeDataService.insert(node);
                ids[i] = Long.toString(node.getId());
            }

            // 创建 PostDO 对象
            PostDO postDO = new PostDO();
            postDO.setContent(String.join(",", ids));
            postDO.setNodeId(request.getNodeId());
            postDO.setType(request.getType());
            postDO.setCreatorId(userId);
            postDO.setState(postState.value());
            postDataService.insert(postDO);
        } else {
            // 非 contents 类型的帖子
            PostDO postDO = new PostDO();
            postDO.setContent(request.getContent());
            postDO.setNodeId(request.getNodeId());
            postDO.setType(request.getType());
            postDO.setCreatorId(userId);
            postDO.setState(postState.value());
            postDO.setCreatedAt(Utils.getLocalDateTime());
            postDataService.insert(postDO);
        }
    }

    /**
     * 更新帖子内容
     *
     * @param id 帖子ID
     * @throws BusinessException 当帖子不存在时抛出异常
     */
    @Transactional
    public void updatePost(Long id, UpdatePostRequest request) {
        if (request == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子对象不能为空");
        }

        PostDO postDO = validateAndGetPost(id);
        postDO.setContent(request.getContent());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postDataService.update(postDO);
    }

    /**
     * 删除帖子（软删除）
     *
     * @param id 帖子ID
     * @throws BusinessException 当帖子不存在时抛出异常
     */
    @Transactional
    public void deletePost(Long id) {
        PostDO postDO = validateAndGetPost(id);
        postDO.setState(Enums.PostState.deleted.value());
        postDO.setUpdatedAt(Utils.getLocalDateTime());
        postDataService.update(postDO);
    }

    /**
     * 审核帖子
     */
    @Transactional
    public PostDTO approvePost(Long id, boolean approve) {
        PostDO postDO = validateAndGetPost(id);

        if (approve && postDO.getState() != Enums.PostState.approved.value()) {
            postDO.setState(Enums.CommentState.approved.value());
            postDataService.update(postDO);
        }
        if (!approve && postDO.getState() != Enums.CommentState.deleted.value()) {
            postDO.setState(Enums.CommentState.deleted.value());
            postDataService.update(postDO);
        }
        return postConverter.toDTO(postDO);
    }

    /**
     * 将帖子内容中的ID转换为名称（向后兼容方法）
     */
    public void idToName(PostDO post) {
        if (post == null || post.getType() == Enums.PostType.article.value() ||
                post.getContent() == null || post.getContent().isEmpty()) {
            return;
        }

        try {
            List<Long> ids = Arrays.stream(post.getContent().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();

            if (!ids.isEmpty()) {
                List<NodeDO> nodeList = nodeDataService.getByIds(ids);
                String names = nodeList.stream().map(NodeDO::getName).collect(Collectors.joining(","));
                post.setContent(names);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse content IDs for post {}: {}", post.getId(), post.getContent(), e);
        }
    }

    // ========== 私有方法 ==========
    
    /**
     * 为PostDTO列表设置完整的关联信息（用户、节点、课程）
     */
    private void setPostDTOAssociations(List<PostDTO> postDTOList) {
        if (postDTOList == null || postDTOList.isEmpty()) {
            return;
        }
        
        // 批量加载用户信息
        List<Long> userIds = Util.getIds(postDTOList, dto -> ((PostDTO) dto).getCreatorId());
        Map<Long, UserDTO> userMap = userService.getUserMap(userIds);
        
        // 批量加载节点信息
        List<Long> nodeIds = Util.getIds(postDTOList, dto -> ((PostDTO) dto).getNodeId());
        Map<Long, NodeDTO> nodeMap = nodeService.getNodeMap(nodeIds);

        // 设置关联信息
        for (PostDTO postDTO : postDTOList) {
            postDTO.setCreator(userMap.get(postDTO.getCreatorId()));
            postDTO.setNode(nodeMap.get(postDTO.getNodeId()));
        }
    }
    
    /**
     * 为PostDTOV2列表设置完整的关联信息（用户、节点、课程、投票状态）
     */
    private void setPostDTOV2Associations(List<PostDTO> postDTOList, Long currentUserId) {
        if (postDTOList == null || postDTOList.isEmpty()) {
            return;
        }
        
        // 批量加载用户信息
        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).distinct().collect(Collectors.toList());
        Map<Long, UserDO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserDO> userList = userDataService.getByIds(userIds);
            userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, user -> user));
        }
        
        // 批量加载节点信息
        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).distinct().collect(Collectors.toList());
        Map<Long, NodeDO> nodeMap = new HashMap<>();
        if (!nodeIds.isEmpty()) {
            List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
            nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));
        }
        
        // 批量加载投票状态
        Map<Long, Integer> voteTypes = new HashMap<>();
        if (currentUserId != null) {
            List<Long> postIds = postDTOList.stream().map(PostDTO::getId).collect(Collectors.toList());
            voteTypes = loadVoteTypes(currentUserId, postIds);
        }
        
        // 设置关联信息
        for (PostDTO postDTO : postDTOList) {
            // 设置用户信息
            postDTO.setCreator(userConverter.toDTOV1(userMap.get(postDTO.getCreatorId())));
            
            // 设置节点信息
            NodeDO nodeDO = nodeMap.get(postDTO.getNodeId());
            if (nodeDO != null) {
                NodeDTO nodeDTOV0 = nodeConverter.toDTO(nodeDO);
                postDTO.setNode(nodeDTOV0);
                
                // 设置课程信息
                if (nodeDTOV0 != null) {
                    nodeDTOV0.setCourse(courseService.getCourseDTOV4ById(nodeDO.getCourseId()));
                }
            }
            
            // 设置投票状态
            if (voteTypes.containsKey(postDTO.getId())) {
                postDTO.setVoteType(voteTypes.get(postDTO.getId()));
            }
        }
    }
    
    /**
     * 处理投票状态
     */
    private Map<Long, Integer> loadVoteTypes(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, Enums.ObjectType.post.value());
        Map<Long, Integer> types = new HashMap<>();
        for (UpvoteDO upvote : upvotes) {
            types.put(upvote.getObjectId(), upvote.getType());
        }
        return types;
    }
    
    /**
     * 解析JSON字符串到字符串列表
     */
    private List<String> parseJsonToStringList(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败: {}", jsonContent, e);
            throw ErrorCode.POSTING_CONTENT_PARSE_FAILED.exception(e);
        }
    }
    
    /**
     * 批量处理内容类型帖子的ID转名称
     */
    private void processContentPostsIdToName(List<PostDO> postings) {
        if (postings == null || postings.isEmpty()) {
            return;
        }
        
        // 收集所有内容ID
        List<Long> allContentIds = postings.stream()
                .filter(post -> post.getType() != Enums.PostType.article.value())
                .map(PostDO::getContent)
                .filter(content -> content != null && !content.isEmpty())
                .flatMap(content -> Arrays.stream(content.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong))
                .distinct()
                .collect(Collectors.toList());
        
        if (allContentIds.isEmpty()) {
            return;
        }
        
        // 批量查询节点信息
        List<NodeDO> nodeList = nodeDataService.getByIds(allContentIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(NodeDO::getId, node -> node));
        
        // 为每个帖子转换内容ID为名称
        for (PostDO postDO : postings) {
            if (postDO.getType() == Enums.PostType.article.value() || 
                postDO.getContent() == null || postDO.getContent().isEmpty()) {
                continue;
            }
            
            String[] contentIds = postDO.getContent().split(",");
            StringBuilder newContent = new StringBuilder();
            
            for (int i = 0; i < contentIds.length; i++) {
                try {
                    long nodeId = Long.parseLong(contentIds[i].trim());
                    NodeDO node = nodeMap.get(nodeId);
                    if (node != null) {
                        if (i > 0) {
                            newContent.append(",");
                        }
                        newContent.append(node.getName());
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse content ID: {}", contentIds[i], e);
                }
            }
            
            postDO.setContent(newContent.toString());
        }
    }


 // ========== 验证方法 ==========

    /**
     * 验证帖子ID
     */
    public void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证并获取帖子
     */
    public PostDO validateAndGetPost(Long postId) {
        validatePostId(postId);
        PostDO postDO = postDataService.getById(postId);
        if (postDO == null) {
            throw ErrorCode.CONTENTS_POST_NOT_FOUND.exception();
        }
        return postDO;
    }

    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
        }
    }

    /**
     * 验证节点ID
     */
    private void validateNodeId(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
        }
    }
}
