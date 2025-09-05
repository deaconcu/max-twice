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
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.dto.request.UpdatePostRequest;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.PostDTO;
import com.prosper.learn.dto.response.PostDTOV2;
import com.prosper.learn.dto.response.UserDTOV1;
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
public class PostingService {

    /** 节点数据访问接口 */
    private final NodeDataService nodeDataService;
    
    /** 帖子数据访问接口 */
    private final PostDataService postDataService;
    
    /** 课程数据访问接口 */
    private final CourseDataService courseDataService;
    
    /** 点赞数据访问接口 */
    private final UpvoteDataService upvoteDataService;
    
    /** 用户数据访问接口 */
    private final UserDataService userDataService;
    
    /** 日常统计服务 */
    private final DailyStatsService dailyStatsService;
    
    /** JSON对象映射器 */
    private final ObjectMapper objectMapper;
    
    /** 系统配置属性 */
    private final SystemProperties systemProperties;

    // ========== 私有验证方法 ==========
    
    /**
     * 验证帖子ID
     */
    private void validatePostId(Long postId) {
        if (postId == null || postId <= 0) {
            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
        }
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
    
    /**
     * 验证并获取帖子
     */
    private PostDO validateAndGetPost(Long postId) {
        validatePostId(postId);
        PostDO postDO = postDataService.getById(postId);
        if (postDO == null) {
            throw ErrorCode.CONTENTS_POST_NOT_FOUND.exception();
        }
        return postDO;
    }
    
    /**
     * 批量加载节点信息
     */
    private Map<Long, NodeDO> loadNodeMap(List<? extends Object> dtos, java.util.function.Function<Object, Long> nodeIdExtractor) {
        List<Long> nodeIds = dtos.stream()
            .map(nodeIdExtractor)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        
        if (nodeIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        return nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));
    }
    
    /**
     * 为PostDTO列表设置完整的关联信息（用户、节点、课程）
     */
    private void setPostDTOAssociations(List<PostDTO> postDTOList) {
        if (postDTOList == null || postDTOList.isEmpty()) {
            return;
        }
        
        // 批量加载用户信息
        Map<Long, UserDO> userMap = loadUserMap(postDTOList, dto -> ((PostDTO) dto).getCreatorId());
        
        // 批量加载节点信息
        Map<Long, NodeDO> nodeMap = loadNodeMap(postDTOList, dto -> ((PostDTO) dto).getNodeId());
        
        // 批量加载课程信息（基于节点）
        List<Long> courseIds = nodeMap.values().stream()
                .map(NodeDO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, CourseDO> courseMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            List<CourseDO> courseList = courseDataService.getByIds(courseIds);
            courseMap = courseList.stream().collect(Collectors.toMap(CourseDO::getId, course -> course));
        }
        
        // 设置关联信息
        for (PostDTO postDTO : postDTOList) {
            // 设置用户信息
            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
            
            // 设置节点信息
            NodeDO nodeDO = nodeMap.get(postDTO.getNodeId());
            if (nodeDO != null) {
                NodeDTO nodeDTO = Converter.INSTANCE.toNodeDTO(nodeDO);
                postDTO.setNode(nodeDTO);
                
                // 设置课程信息
                if (nodeDTO != null && nodeDO.getCourseId() != null) {
                    nodeDTO.setCourse(Converter.INSTANCE.toCourseDTOV4(courseMap.get(nodeDO.getCourseId())));
                }
            }
        }
    }
    
    /**
     * 为PostDTOV2列表设置完整的关联信息（用户、节点、课程、投票状态）
     */
    private void setPostDTOV2Associations(List<PostDTOV2> postDTOList, Long currentUserId) {
        if (postDTOList == null || postDTOList.isEmpty()) {
            return;
        }
        
        // 批量加载用户信息
        List<Long> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).distinct().collect(Collectors.toList());
        Map<Long, UserDO> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<UserDO> userList = userDataService.getByIds(userIds);
            userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, user -> user));
        }
        
        // 批量加载节点信息
        List<Long> nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).distinct().collect(Collectors.toList());
        Map<Long, NodeDO> nodeMap = new HashMap<>();
        if (!nodeIds.isEmpty()) {
            List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
            nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));
        }
        
        // 批量加载投票状态
        Map<Long, Integer> voteTypes = new HashMap<>();
        if (currentUserId != null) {
            List<Long> postIds = postDTOList.stream().map(PostDTOV2::getId).collect(Collectors.toList());
            voteTypes = loadVoteTypes(currentUserId, postIds);
        }
        
        // 设置关联信息
        for (PostDTOV2 postDTO : postDTOList) {
            // 设置用户信息
            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
            
            // 设置节点信息
            NodeDO nodeDO = nodeMap.get(postDTO.getNodeId());
            if (nodeDO != null) {
                NodeDTO nodeDTO = Converter.INSTANCE.toNodeDTO(nodeDO);
                postDTO.setNode(nodeDTO);
                
                // 设置课程信息
                if (nodeDTO != null) {
                    nodeDTO.setCourse(Converter.INSTANCE.toCourseDTOV4(courseDataService.getById(nodeDO.getCourseId())));
                }
            }
            
            // 设置投票状态
            if (voteTypes.containsKey(postDTO.getId())) {
                postDTO.setVoteType(voteTypes.get(postDTO.getId()));
            }
        }
    }
    
    /**
     * 批量加载用户信息
     */
    private Map<Long, UserDO> loadUserMap(List<? extends Object> dtos, java.util.function.Function<Object, Long> userIdExtractor) {
        List<Long> userIds = dtos.stream()
            .map(userIdExtractor)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        List<UserDO> userList = userDataService.getByIds(userIds);
        return userList.stream().collect(Collectors.toMap(UserDO::getId, user -> user));
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
    
    /**
     * 处理内容解析（ID转名称）
     */
    private void processContentIdToName(PostDO posting) {
        if (posting == null || posting.getType() == Enums.PostType.article.value() || 
            posting.getContent() == null || posting.getContent().isEmpty()) {
            return;
        }
        
        try {
            List<Long> ids = Arrays.stream(posting.getContent().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
            
            if (!ids.isEmpty()) {
                List<NodeDO> nodeList = nodeDataService.getByIds(ids);
                String names = nodeList.stream().map(NodeDO::getName).collect(Collectors.joining(","));
                posting.setContent(names);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse content IDs for post {}: {}", posting.getId(), posting.getContent(), e);
        }
    }

    /**
     * 获取单个帖子
     * 
     * @param id 帖子ID
     * @return 帖子对象（已转换内容ID为名称）
     * @throws BusinessException 当帖子不存在时抛出异常
     */
    public PostDO get(long id) {
        PostDO posting = validateAndGetPost(id);
        processContentIdToName(posting);
        return posting;
    }

    /**
     * 批量获取帖子列表
     * 
     * @param ids 帖子ID列表
     * @return 帖子列表
     */
    public List<PostDO> getList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PostDO> postings = postDataService.getByIds(ids);
        postings.forEach(this::processContentIdToName);
        return postings;
    }

    /**
     * 获取节点下的帖子列表（按分数排序）
     * 
     * @param nodeId 节点ID
     * @return 帖子列表
     * @throws BusinessException 当节点ID无效时抛出异常
     */
    public List<PostDO> getList(long nodeId) {
        validateNodeId(nodeId);
        List<PostDO> postings = postDataService.getListByNodeAndScore(nodeId, systemProperties.getPosting().getDefaultNodePostCount(), Enums.PostState.approved.value());
        postings.forEach(this::processContentIdToName);
        return postings;
    }

    /**
     * 获取节点下的帖子列表（分页）
     * 
     * @param nodeId 节点ID
     * @param lastPostingId 最后一个帖子ID
     * @return 帖子列表
     * @throws BusinessException 当参数无效时抛出异常
     */
    public List<PostDO> getList(long nodeId, long lastPostingId) {
        validateNodeId(nodeId);
        if (lastPostingId < 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("最后帖子ID无效: " + lastPostingId);
        }
        
        List<PostDO> postings = postDataService.getListByLastId(nodeId, lastPostingId, systemProperties.getPosting().getDefaultNodePostCount(), Enums.PostState.approved.value());
        postings.forEach(this::processContentIdToName);
        return postings;
    }

    /**
     * 将帖子内容中的ID转换为名称（向后兼容方法）
     * 
     * @param posting 帖子对象
     */
    public void idToName(PostDO posting) {
        processContentIdToName(posting);
    }

    /**
     * 获取用户的文章列表
     * 
     * @param userId 用户ID
     * @param lastId 最后一个帖子ID
     * @return 文章DTO列表
     * @throws BusinessException 当用户ID无效时抛出异常
     */
    public List<PostDTO> getUserArticle(long userId, long lastId) {
        validateUserId(userId);
        if (lastId < 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("最后帖子ID无效: " + lastId);
        }
        
        List<PostDO> postings = postDataService.getArticleListByUser(userId, lastId, systemProperties.getPosting().getDefaultPageSize());
        if (postings == null || postings.isEmpty()) {
            return new ArrayList<>();
        }

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postings);

        // 批量加载用户信息
        Map<Long, UserDO> userMap = loadUserMap(postDTOList, dto -> ((PostDTO) dto).getCreatorId());

        // 批量加载节点信息
        Map<Long, NodeDO> nodeMap = loadNodeMap(postDTOList, dto -> ((PostDTO) dto).getNodeId());

        for (PostDTO postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            if (node != null) {
                node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseDataService.getById(node.getCourseId())));
            }

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }

        return postDTOList;
    }

    public List<PostDTO> getUserContents(long userId, long lastId) {
        validateUserId(userId);
        if (lastId < 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("最后帖子ID无效: " + lastId);
        }
        
        int count = systemProperties.getPosting().getUserContentsPageSize();
        List<PostDO> postings = postDataService.getContentsListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        // 使用提取的公共方法处理内容ID转名称
        processContentPostsIdToName(postings);

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postings);
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
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseDataService.getById(node.getCourseId())));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * 基于分数的排序获取文章列表
     * @param nodeId 节点ID
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    public List<PostDO> getListByScore(long nodeId, int limit) {
        List<PostDO> postings = postDataService.getListByNodeAndScore(nodeId, limit, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return postings;
    }

    /**
     * 基于分数的分页查询
     * @param nodeId 节点ID
     * @param lastScore 上一页最后一个文章的分数
     * @param lastId 上一页最后一个文章的ID
     * @param limit 返回数量限制
     * @return 按分数排序的文章列表
     */
    public List<PostDO> getListByScoreWithPagination(long nodeId, Double lastScore, long lastId, int limit) {
        if (lastScore == null) {
            return getListByScore(nodeId, limit);
        }

        List<PostDO> postings = postDataService.getListByNodeAndScoreAndPaginated(
                nodeId, lastScore, lastId, limit, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return postings;
    }

    /**
     * 获取用户文章列表（包含阅读量）
     */
    public List<PostDTOV2> getUserArticleWithViews(long userId, long lastId) {
        int count = systemProperties.getPosting().getUserContentsPageSize();
        List<PostDO> postings = postDataService.getArticleListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        List<PostDTOV2> postDTOList = Converter.INSTANCE.toPostDTOV2(postings);

        // 设置views字段
        setViewsForPosts(postDTOList);

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        // get all node
        List<Long> nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        for (PostDTOV2 postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseDataService.getById(node.getCourseId())));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }

        return postDTOList;
    }

    /**
     * 获取用户目录列表（包含阅读量）
     */
    public List<PostDTOV2> getUserContentsWithViews(long userId, long lastId) {
        int count = systemProperties.getPosting().getUserContentsPageSize();
        List<PostDO> postings = postDataService.getContentsListByUser(userId, lastId, count);
        if (postings == null || postings.size() == 0) return new ArrayList<>();

        // 使用提取的公共方法处理内容ID转名称
        processContentPostsIdToName(postings);

        List<PostDTOV2> postDTOList = Converter.INSTANCE.toPostDTOV2(postings);
        
        // 设置views字段
        setViewsForPosts(postDTOList);
        
        List<Long> nodeIds = postDTOList.stream().map(PostDTOV2::getNodeId).collect(Collectors.toList());
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
        List<Long> userIds = postDTOList.stream().map(PostDTOV2::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostDTOV2 postDTO : postDTOList) {
            postDTO.setNode(Converter.INSTANCE.toNodeDTO(nodeMap.get(postDTO.getNodeId())));
            NodeDTO node = postDTO.getNode();
            node.setCourse(Converter.INSTANCE.toCourseDTOV4(courseDataService.getById(node.getCourseId())));

            if (types.containsKey(postDTO.getId()))
                postDTO.setVoteType(types.get(postDTO.getId()));

            postDTO.setCreator(Converter.INSTANCE.toUserDTOV1(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * 为文章列表设置阅读量（历史数据 + 今日实时数据）
     */
    private void setViewsForPosts(List<PostDTOV2> postDTOList) {
        // 使用DailyStatsService来设置阅读量
        dailyStatsService.setViewsForPosts(postDTOList);
    }

    /**
     * 批量获取帖子（带用户信息和投票状态）
     */
    public List<PostDTO> getPostsWithUserAndVoteInfo(List<Long> ids, Long nodeId, double lastScore, Long lastPostingId, long currentUserId) {
        List<PostDO> postDOList = null;
        if (ids != null && ids.size() > 0) {
            postDOList = postDataService.getByIds(ids);
        } else if (nodeId != null && nodeId > 0) {
            int count = 2;
            postDOList = postDataService.getListByNodeAndScoreAndPaginated(nodeId, lastScore, lastPostingId, count, Enums.PostState.approved.value());
        }
        
        if (postDOList == null) {
            throw ErrorCode.POSTING_LIST_QUERY_FAILED.exception();
        }

        List<Long> allPostingIds = new ArrayList<>();
        List<Long> userIds = new LinkedList<>();
        postDOList.forEach(postingDO -> {
            idToName(postingDO);
            allPostingIds.add(postingDO.getId());
            userIds.add(postingDO.getCreator());
        });

        List<UserDTOV1> userList = userIds.size() == 0 ?
                new ArrayList<>() : Converter.INSTANCE.toUserDTOV1(userDataService.getByIds(userIds));
        Map<Long, UserDTOV1> userMap = new HashMap<>();
        for (UserDTOV1 user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostDTO> postDTOList = Converter.INSTANCE.toPostDTO(postDOList);
        postDTOList.stream().forEach(item -> {
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(currentUserId, allPostingIds, Enums.ObjectType.post.value());
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

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     * 
     * @param request 帖子创建请求对象
     * @throws BusinessException 当节点不存在或JSON处理失败时抛出异常
     */
    @Transactional
    public void createPost(long userId, CreatePostRequest request) {
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
            
            for (int i = 0; i < nodeNames.size(); i++) {
                NodeDO node = new NodeDO();
                node.setName(nodeNames.get(i));
                node.setDescription("");
                node.setRoot(0L);
                node.setCourseId(nodeDO.getCourseId());
                node.setCreatedAt(Utils.getLocalDateTime());
                node.setUpdatedAt(Utils.getLocalDateTime());
                nodeDataService.insert(node);
                ids[i] = Long.toString(node.getId());
            }
            
            // 创建 PostDO 对象
            PostDO postDO = new PostDO();
            postDO.setContent(String.join(",", ids));
            postDO.setNodeId(request.getNodeId());
            postDO.setType(request.getType());
            postDO.setCreator(userId);
            postDO.setCreatedAt(Utils.getLocalDateTime());
            postDataService.insert(postDO);
        } else {
            // 非 contents 类型的帖子
            PostDO postDO = new PostDO();
            postDO.setContent(request.getContent());
            postDO.setNodeId(request.getNodeId());
            postDO.setType(request.getType());
            postDO.setCreator(userId);
            postDO.setCreatedAt(Utils.getLocalDateTime());
            postDataService.insert(postDO);
        }
    }

    /**
     * 更新帖子内容
     * 
     * @param id 帖子ID
     * @param posting 帖子DTO对象
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
     * 获取帖子详情（转换为DTO）
     */
    public PostDTO getPostDetail(Long id) {
        return Converter.INSTANCE.toPostDTO(get(id));
    }

    /**
     * 获取节点帖子列表
     */
    public List<PostDTO> getNodePostsList(Long nodeId) {
        int count = systemProperties.getPosting().getDefaultNodeListCount();
        List<PostDO> postings = postDataService.getListByNode(nodeId, count, Enums.PostState.approved.value());
        postings.forEach(this::idToName);
        return Converter.INSTANCE.toPostDTO(postings);
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
        return Converter.INSTANCE.toPostDTO(postDOList);
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
        return Converter.INSTANCE.toPostDTO(postDO);
    }
}
