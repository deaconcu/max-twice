package com.prosper.learn.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.service.DailyStatsService;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.NodeConverter;
import com.prosper.learn.application.converter.PostConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.request.CreatePostRequest;
import com.prosper.learn.application.dto.request.MarkImageUsedRequest;
import com.prosper.learn.application.dto.request.UpdatePostRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.NodeDTO;
import com.prosper.learn.application.dto.response.PostDTO;
import com.prosper.learn.application.dto.response.node.NodeWithCourseBriefDTO;
import com.prosper.learn.application.dto.response.post.PostFullDTO;
import com.prosper.learn.application.dto.response.post.PostSummaryDTO;
import com.prosper.learn.application.dto.response.post.PostWithVoteDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.post.PostDomainService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 帖子应用服务
 * 负责跨域协调、DTO转换、用户验证、事件发布
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

    // 领域服务
    private final PostDomainService domainService;

    // 数据服务
    private final NodeDataService nodeDataService;
    private final PostDataService postDataService;
    private final CourseDataService courseDataService;
    private final UpvoteDataService upvoteDataService;
    private final UserDataService userDataService;
    private final DailyStatsService dailyStatsService;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

    // 工具和配置
    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;

    // 转换器
    private final UserConverter userConverter;
    private final NodeConverter nodeConverter;
    private final CourseConverter courseConverter;
    private final PostConverter postConverter;

    // 其他 ApplicationService
    private final CourseService courseService;
    private final NodeService nodeService;
    private final UserService userService;
    private final ImageUploadService imageUploadService;

    // =========== 公共方法 DTO ==========

    /**
     * 转换为帖子摘要 DTO（基础信息）
     */
    public PostSummaryDTO toSummaryDTO(PostDO postDO) {
        return postConverter.toSummaryDTO(postDO);
    }

    public List<PostSummaryDTO> toSummaryDTO(List<PostDO> postDOList) {
        return postConverter.toSummaryDTO(postDOList);
    }

    @Deprecated
    PostDTO toDTO(PostDO postDO) {
        return postConverter.toDTO(postDO);
    }

    @Deprecated
    List<PostDTO> toDTO(List<PostDO> postDOList) {
        return postConverter.toDTO(postDOList);
    }

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 转换为帖子（含创建者信息）
//     * 用途：基础帖子展示
//     * 替代：原 V1
//     */
//    PostWithCreatorDTO toPostWithCreator(PostDO postDO) {
//        PostWithCreatorDTO postDTO = postConverter.toWithCreatorDTO(postDO);
//        postDTO.setCreator(userService.toBriefDTO(userDataService.getById(postDO.getCreatorId())));
//        return postDTO;
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:18):
//    List<PostWithCreatorDTO> toPostWithCreator(List<PostDO> postDOList) {
//        return postConverter.toWithCreatorDTO(postDOList);
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 转换为完整帖子信息（含节点、创建者、浏览量、投票类型）
//     * 用途：帖子详情页、帖子列表（完整信息）
//     */
//    PostFullDTO toPostWithFullInfo(PostDO postDO) {
//        return postConverter.toFullDTO(postDO);
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

    List<PostFullDTO> toPostWithFullInfo(List<PostDO> postDOList, long userId) {
        List<PostFullDTO> postDTOList = postConverter.toFullDTO(postDOList);

        // 批量获取统计数据
        List<Long> postIds = postDTOList.stream().map(PostSummaryDTO::getId).collect(Collectors.toList());
        Map<Long, ContentStatsDTO> statsMap = dailyStatsService.batchGetContentStats(ContentType.post, postIds);

        // 填充统计字段
        postDTOList.forEach(post -> {
            ContentStatsDTO stats = statsMap.get(post.getId());
            if (stats != null) {
                post.setViewCount(stats.getViews());
                post.setTwice(stats.getTwiceUpvotes());
                post.setHelpful(stats.getLikeUpvotes());
                post.setCommentCount(stats.getComments());
            }
        });

        List<Long> nodeIds = postDTOList.stream().map(PostSummaryDTO::getNodeId).collect(Collectors.toList());
        List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
        Map<Long, NodeDO> nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));

        List<Long> allPostingIds = new LinkedList<>();
        if (postDOList != null) postDOList.stream().forEach(item -> allPostingIds.add(item.getId()));

        Map<Long, Integer> types = new HashMap<>();
        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, ContentType.post.value());
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }
        }

        // get all user
        List<Long> userIds = postDTOList.stream().map(PostSummaryDTO::getCreatorId).collect(Collectors.toList());
        List<UserDO> userList = userDataService.getByIds(userIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, node -> node));

        for (PostFullDTO postDTO : postDTOList) {
            // 转换并填充节点信息
            NodeDO nodeDO = nodeMap.get(postDTO.getNodeId());
            if (nodeDO != null) {
                NodeWithCourseBriefDTO nodeDTO = nodeConverter.toWithCourseBriefDTO(nodeDO);
                // 填充课程简要信息
                CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
                if (courseDO != null) {
                    nodeDTO.setCourse(courseService.toBriefDTO(courseDO));
                }
                postDTO.setNode(nodeDTO);
            }

            // 填充投票状态
            if (types.containsKey(postDTO.getId())) {
                postDTO.setVoteType(types.get(postDTO.getId()));
            }

            // 填充创建者信息
            postDTO.setCreator(userConverter.toBriefDTO(userMap.get(postDTO.getCreatorId())));
        }
        return postDTOList;
    }

    /**
     * 转换为帖子（含创建者和投票类型）
     * 用途：帖子列表（轻量级，不含完整节点信息）
     * 替代：原 V3
     */
    List<PostWithVoteDTO> toPostWithVote(List<PostDO> postDOList, long userId) {
        List<Long> allPostingIds = new ArrayList<>();
        List<Long> userIds = new LinkedList<>();
        postDOList.forEach(postingDO -> {
            idToName(postingDO);
            allPostingIds.add(postingDO.getId());
            userIds.add(postingDO.getCreatorId());
        });

        List<UserBriefDTO> userList = userIds.size() == 0 ?
                new ArrayList<>() : userConverter.toBriefDTO(userDataService.getByIds(userIds));
        Map<Long, UserBriefDTO> userMap = new HashMap<>();
        for (UserBriefDTO user : userList) {
            userMap.put(user.getId(), user);
        }

        List<PostWithVoteDTO> postDTOList = postConverter.toWithVoteDTO(postDOList);
        postDTOList.stream().forEach(item -> {
            item.setCreator(userMap.get(item.getCreatorId()));
        });

        if (allPostingIds.size() > 0) {
            List<UpvoteDO> upvotes = upvoteDataService.getList(userId, allPostingIds, ContentType.post.value());
            Map<Long, Integer> types = new HashMap<>();
            for (UpvoteDO upvote : upvotes) {
                types.put(upvote.getObjectId(), upvote.getType());
            }

            for (PostWithVoteDTO posting : postDTOList) {
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
        return domainService.getWithIdToName(id);
    }

    /**
     * 获取单个帖子DTO
     */
    public PostSummaryDTO getDTO(long id) {
        PostDO post = domainService.getWithIdToName(id);
        return postConverter.toSummaryDTO(post);
    }

// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 批量获取帖子列表
//     */
//    public List<PostSummaryDTO> getDTOList(List<Long> ids) {
//        if (ids == null || ids.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        List<PostDO> postings = domainService.getByIdsWithIdToName(ids);
//        return postConverter.toSummaryDTO(postings);
//    }
// --注释掉检查 STOP (2025/12/10 11:17)

    /**
     * 获取节点下的帖子列表（按分数排序），获取前 N 条
     */
    public List<PostDO> getList(long nodeId) {
        nodeDataService.validateAndGet(nodeId);
        return domainService.getListByNodeAndScore(
                nodeId, systemProperties.getPosting().getDefaultNodePostCount(), ContentState.PUBLISHED.value());
    }

    /**
     * 获取用户文章或目录
     * @param userId 用户ID
     * @param lastId 分页游标
     * @param postType 帖子类型
     * @param state 状态过滤（null表示所有状态，否则只返回指定状态）
     */
    public List<PostFullDTO> getUserPosts(Long userId, Long lastId, PostType postType, Byte state) {
        userDataService.validateAndGet(userId);

        int count = systemProperties.getPosting().getUserContentsPageSize();

        // 调用 DomainService 查询（包含 idToName 处理）
        List<PostDO> postings = domainService.getUserPosts(userId, postType.value(), lastId, state, count);
        if (postings == null || postings.isEmpty()) {
            return new ArrayList<>();
        }

        return toPostWithFullInfo(postings, userId);
    }

    /**
     * 获取用户帖子（带分页信息）
     */
    public KeysetPageResponse<PostFullDTO> getUserPostsWithPagination(Long userId, Long lastId, PostType postType, Byte state) {
        userDataService.validateAndGet(userId);

        int count = systemProperties.getPosting().getUserContentsPageSize();

        // 调用 DomainService 查询（包含 idToName 处理）
        List<PostDO> postings = domainService.getUserPosts(userId, postType.value(), lastId, state, count + 1);
        if (postings == null || postings.isEmpty()) {
            return KeysetPageResponse.of(new ArrayList<>(), false, null, null);
        }

        // 判断是否还有更多数据
        boolean hasMore = postings.size() > count;
        if (hasMore) {
            postings = postings.subList(0, count);
        }

        List<PostFullDTO> dtoList = toPostWithFullInfo(postings, userId);

        // 获取最后一项的ID
        Long nextLastId = hasMore && !dtoList.isEmpty() ? dtoList.get(dtoList.size() - 1).getId() : null;

        return KeysetPageResponse.of(dtoList, hasMore, null, nextLastId);
    }


    /**
     * 按 IDs 批量获取帖子（带用户信息和投票状态）
     */
    public List<PostWithVoteDTO> getPostsByIds(List<Long> ids, long userId) {
        // 调用 DomainService 查询（带 idToName 处理）
        List<PostDO> postDOList = domainService.getByIdsWithIdToName(ids);

        // DTO 转换（填充用户和点赞信息）
        return toPostWithVote(postDOList, userId);
    }

    /**
     * 获取节点帖子分页列表（带用户信息和投票状态）
     * 返回 KeysetPageResponse 格式
     */
    public KeysetPageResponse<PostWithVoteDTO> getNodePostsPage(
            Long nodeId, Double lastScore, Long lastPostingId, long userId) {
        int pageSize = 20; // 每页数量

        // 查询 pageSize + 1 条数据，用于判断是否有更多
        List<PostDO> postDOList = domainService.getPostsByIdsOrNode(
                null, nodeId, lastScore, lastPostingId, pageSize + 1, ContentState.PUBLISHED.value());

        // 判断是否有更多数据
        boolean hasMore = postDOList.size() > pageSize;
        if (hasMore) {
            postDOList = postDOList.subList(0, pageSize); // 只返回 pageSize 条
        }

        // DTO 转换
        List<PostWithVoteDTO> items = toPostWithVote(postDOList, userId);

        // 构建 nextCursor
        Double nextLastScore = null;
        Long nextLastId = null;
        if (hasMore && !items.isEmpty()) {
            PostWithVoteDTO lastItem = items.get(items.size() - 1);
            nextLastScore = lastItem.getScore();
            nextLastId = lastItem.getId();
        }

        return KeysetPageResponse.of(items, hasMore, nextLastScore, nextLastId);
    }

    /**
     * 获取节点帖子列表
     */
    public List<PostSummaryDTO> getNodePostsList(Long nodeId) {
        int count = systemProperties.getPosting().getDefaultNodeListCount();

        // 调用 DomainService 查询（包含 idToName 处理）
        List<PostDO> postings = domainService.getNodePostsList(nodeId, count, ContentState.PUBLISHED.value());

        return toSummaryDTO(postings);
    }

    /**
     * 根据状态获取帖子列表
     */
    public List<PostSummaryDTO> getPostsByState(ContentState state) {
        int limit = systemProperties.getPosting().getPendingPostsLimit();

        // 调用 DomainService 查询（包含 idToName 处理）
        List<PostDO> postDOList = domainService.getListByState(state.value(), limit);

        return toSummaryDTO(postDOList);
    }

    /**
     * 根据状态获取帖子列表（支持分页）
     */
    public List<PostSummaryDTO> getPostsByState(ContentState state, Long lastId, Integer limit) {
        // 调用 DomainService 查询（包含 idToName 处理）
        List<PostDO> postDOList = domainService.getListByState(state.value(), lastId, limit);

        return toSummaryDTO(postDOList);
    }

    /**
     * 获取待审核帖子列表
     */
    public List<PostSummaryDTO> getPendingPostsList() {
        return getPostsByState(ContentState.SUBMITTED);
    }

    /**
     * 根据节点、用户和状态筛选帖子列表
     */
    public List<PostSummaryDTO> getPostsByNodeAndCreator(Long nodeId, Long creatorId, Long lastId, Byte state) {
        int limit = systemProperties.getPosting().getPendingPostsLimit();

        // 调用 DomainService 查询（已包含 idToName 处理）
        List<PostDO> postDOList = domainService.getListByNodeAndCreator(nodeId, creatorId, lastId, state, limit);

        return toSummaryDTO(postDOList);
    }



    // ========== 公共方法 command ==========

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     *
     * @param currentUser 当前用户
     * @param request 帖子创建请求对象
     * @throws BusinessException 当节点不存在或JSON处理失败时抛出异常
     */
    @Transactional
    public void createPost(UserDO currentUser, CreatePostRequest request) {
        createPost(currentUser, request, ContentState.SUBMITTED);
    }

    /**
     * 创建帖子（处理contents类型的特殊逻辑）
     *
     * @param currentUser 当前用户
     * @param request 帖子创建请求对象
     * @param postState 帖子状态
     * @throws BusinessException 当节点不存在或JSON处理失败时抛出异常
     */
    @Transactional
    public Long createPost(UserDO currentUser, CreatePostRequest request, ContentState postState) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子对象不能为空");
        }
        if (currentUser == null || currentUser.getId() == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户信息无效");
        }

        long userId = currentUser.getId();

        // 根据类型调用不同的 DomainService 方法
        Long postId;
        if (request.getType() == PostType.contents.value()) {
            postId = domainService.createContentsPost(userId, request.getNodeId(), request.getContent(), postState);
        } else {
            postId = domainService.createArticlePost(userId, request.getNodeId(), request.getType(), request.getContent(), postState);
        }

        // 自动标记内容中的图片为使用中
        markImagesAsUsed(request.getContent(), "post", postId);

        return postId;
    }

    /**
     * 更新帖子内容
     *
     * @param id 帖子ID
     * @param request 更新请求
     * @param operator 操作用户
     * @throws BusinessException 当帖子不存在或帖子类型为目录时抛出异常
     */
    @Transactional
    public void updatePost(Long id, UpdatePostRequest request, UserDO operator) {
        if (request == null) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子对象不能为空");
        }

        PostDO postDO = domainService.validateAndGet(id);

        // 验证权限：只有所有者或管理员可以修改
        if (!postDO.getCreatorId().equals(operator.getId()) && !operator.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        // 调用 DomainService 更新
        domainService.updatePost(id, request.getContent());

        // 修改后重新设置为待审核状态
        domainService.updateState(id, ContentState.SUBMITTED, null);

        // 自动标记内容中的图片为使用中
        markImagesAsUsed(request.getContent(), "post", id);
    }

    /**
     * 更新帖子内容并返回更新后的DTO
     *
     * @param id 帖子ID
     * @param request 更新请求
     * @param currentUser 当前用户
     * @return 更新后的帖子DTO
     * @throws BusinessException 当帖子不存在或帖子类型为目录时抛出异常
     */
    @Transactional
    public PostSummaryDTO updatePostAndReturn(Long id, UpdatePostRequest request, UserDO currentUser) {
        updatePost(id, request, currentUser);
        PostDO postDO = domainService.validateAndGet(id);
        return postConverter.toSummaryDTO(postDO);
    }

    /**
     * 删除帖子（软删除）
     *
     * @param id 帖子ID
     * @param currentUser 当前用户
     * @throws BusinessException 当帖子不存在时抛出异常
     */
    @Transactional
    public void deletePost(Long id, UserDO currentUser) {
        PostDO postDO = domainService.validateAndGet(id);

        // 验证权限：只有所有者或管理员可以删除
        if (!postDO.getCreatorId().equals(currentUser.getId()) && !currentUser.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        domainService.softDelete(id);

        log.info("用户 {} 删除了帖子 {}", currentUser.getId(), id);
    }

// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 审核帖子
//     */
//    @Transactional
//    public PostSummaryDTO approvePost(Long id, boolean approve) {
//        PostDO postDO = validateAndGetPost(id);
//
//        if (approve && postDO.getState() != ContentState.PUBLISHED.value()) {
//            postDO.setState(ContentState.PUBLISHED.value());
//            postDataService.update(postDO);
//        }
//        if (!approve && postDO.getState() != ContentState.REJECTED.value()) {
//            postDO.setState(ContentState.REJECTED.value());
//            postDataService.update(postDO);
//        }
//        return postConverter.toSummaryDTO(postDO);
//    }
// --注释掉检查 STOP (2025/12/10 11:17)

    /**
     * 批准帖子
     *
     * @param id 帖子ID
     * @param currentUser 当前审核员
     */
    @Transactional
    public void approve(Long id, UserDO currentUser) {
        PostDO postDO = domainService.validateAndGet(id);

        // 调用 DomainService 更新状态
        domainService.approve(id);

        // 发布审核通过事件，触发统计更新和通知
        eventPublisher.publishEvent(ContentApprovedEvent.forPost(
            postDO.getCreatorId(),
            postDO.getId(),
            null,  // postPreview - approve 不需要
            postDO.getNodeId(),  // nodeId - 用于统计
            null,  // nodeName
            null,  // courseName
            postDO.getType()  // postType - 用于区分 ARTICLE/INDEX
        ));

        log.info("审核员 {} 批准了帖子 {}", currentUser.getId(), id);
    }

    /**
     * 拒绝帖子（审核不通过）
     *
     * @param id 帖子ID
     * @param reason 拒绝原因
     * @param currentUser 当前审核员
     */
    @Transactional
    public void reject(Long id, String reason, UserDO currentUser) {
        // 获取帖子信息
        PostDO postDO = postDataService.validateAndGet(id);

        NodeDO nodeDO = nodeDataService.getById(postDO.getNodeId());
        CourseDO courseDO = nodeDO != null ? courseDataService.getById(nodeDO.getCourseId()) : null;

        // 截取内容前50个字符作为预览
        String contentPreview = Utils.stripFormatting(postDO.getContent());
        if (contentPreview != null && contentPreview.length() > 50) {
            contentPreview = contentPreview.substring(0, 50) + "...";
        }

        postDataService.reject(id, reason);

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forPost(
                postDO.getCreatorId(),
                postDO.getId(),
                contentPreview,
                nodeDO != null ? nodeDO.getId() : null,
                nodeDO != null ? nodeDO.getName() : null,
                courseDO != null ? courseDO.getName() : null,
                reason
        ));

        log.info("审核员 {} 拒绝了帖子 {}, 原因: {}", currentUser.getId(), id, reason);
    }

    /**
     * 下架帖子（已发布内容违规，降级为REJECTED状态）
     *
     * @param id 帖子ID
     * @param reason 下架原因
     * @param currentUser 当前审核员
     */
    @Transactional
    public void remove(Long id, String reason, UserDO currentUser) {
        PostDO postDO = postDataService.validateAndGet(id);

        // 检查状态：只能下架已发布的内容
        if (postDO.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能下架已发布的内容");
        }

        NodeDO nodeDO = nodeDataService.getById(postDO.getNodeId());
        CourseDO courseDO = nodeDO != null ? courseDataService.getById(nodeDO.getCourseId()) : null;

        // 截取内容前50个字符作为预览
        String contentPreview = Utils.stripFormatting(postDO.getContent());
        if (contentPreview != null && contentPreview.length() > 50) {
            contentPreview = contentPreview.substring(0, 50) + "...";
        }

        // 更新状态为 REJECTED
        postDataService.reject(id, reason);

        // 发布内容下架事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentRemovedEvent.forPost(
            postDO.getCreatorId(),
            postDO.getId(),
            postDO.getNodeId(),
            postDO.getType(),
            contentPreview,
            nodeDO != null ? nodeDO.getName() : null,
            courseDO != null ? courseDO.getName() : null,
            reason
        ));

        log.info("审核员 {} 下架了帖子 {}, 原因: {}", currentUser.getId(), id, reason);
    }

    /**
     * 恢复帖子（管理员撤销误操作）
     *
     * @param id 帖子ID
     * @param reason 恢复原因
     * @param currentUser 当前管理员
     */
    @Transactional
    public void restore(Long id, String reason, UserDO currentUser) {
        PostDO postDO = postDataService.validateAndGet(id);

        // 记录之前的状态
        Byte previousState = postDO.getState();

        // 检查状态：只能恢复 REJECTED 或 BANNED 的内容
        if (previousState != ContentState.REJECTED.value() && previousState != ContentState.BANNED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能恢复被拒绝或被封禁的内容");
        }

        // 从 BANNED 恢复需要 ADMIN 权限
        if (previousState == ContentState.BANNED.value() && !currentUser.hasRole(UserRole.ADMIN)) {
            throw StatusCode.PERMISSION_DENIED.exception("只有管理员可以解封内容");
        }

        NodeDO nodeDO = nodeDataService.getById(postDO.getNodeId());
        CourseDO courseDO = nodeDO != null ? courseDataService.getById(nodeDO.getCourseId()) : null;

        // 截取内容前50个字符作为预览
        String contentPreview = Utils.stripFormatting(postDO.getContent());
        if (contentPreview != null && contentPreview.length() > 50) {
            contentPreview = contentPreview.substring(0, 50) + "...";
        }

        // 恢复为 PUBLISHED 状态
        domainService.updateState(id, ContentState.PUBLISHED, null);

        // 发布内容恢复事件，触发统计恢复和消息通知
        eventPublisher.publishEvent(ContentRestoredEvent.forPost(
            currentUser.getId(),  // operatorId
            postDO.getCreatorId(),
            postDO.getId(),
            previousState,
            postDO.getNodeId(),
            postDO.getType(),
            contentPreview,
            nodeDO != null ? nodeDO.getName() : null,
            courseDO != null ? courseDO.getName() : null,
            reason
        ));

        log.info("管理员 {} 恢复了帖子 {}, 原因: {}", currentUser.getId(), id, reason);
    }

    /**
     * 封禁帖子（违规封禁）
     *
     * @param id 帖子ID
     * @param reason 封禁原因
     * @param currentUser 当前审核员
     */
    @Transactional
    public void ban(Long id, String reason, UserDO currentUser) {
        PostDO postDO = postDataService.validateAndGet(id);

        // 记录之前的状态
        Byte previousState = postDO.getState();

        NodeDO nodeDO = nodeDataService.getById(postDO.getNodeId());
        CourseDO courseDO = nodeDO != null ? courseDataService.getById(nodeDO.getCourseId()) : null;

        // 截取内容前50个字符作为预览
        String contentPreview = Utils.stripFormatting(postDO.getContent());
        if (contentPreview != null && contentPreview.length() > 50) {
            contentPreview = contentPreview.substring(0, 50) + "...";
        }

        // 更新状态为 BANNED
        postDataService.ban(id, reason);

        // 发布内容封禁事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentBannedEvent.forPost(
            postDO.getCreatorId(),
            postDO.getId(),
            previousState,
            postDO.getNodeId(),
            postDO.getType(),
            contentPreview,
            nodeDO != null ? nodeDO.getName() : null,
            courseDO != null ? courseDO.getName() : null,
            reason
        ));

        log.info("审核员 {} 封禁了帖子 {}, 原因: {}", currentUser.getId(), id, reason);
    }

// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 拒绝帖子（审核不通过）- 无原因版本
//     */
//    @Transactional
//    public void rejectPost(Long id) {
//        validatePostId(id);
//        postDataService.reject(id);
//    }
// --注释掉检查 STOP (2025/12/10 11:17)

    /**
     * 封禁帖子（违规封禁）- 无原因版本
     */
    @Transactional
    public void banPost(Long id) {
        postDataService.validateAndGet(id);
        postDataService.ban(id);
    }

    /**
     * 将帖子内容中的ID转换为名称（向后兼容方法）
     */
    /**
     * 将目录型帖子的内容ID转换为节点信息（委托给 DomainService）
     */
    public void idToName(PostDO post) {
        domainService.processIdToName(post);
    }

    // ========== 私有方法 ==========
    
// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 为PostDTO列表设置完整的关联信息（用户、节点、课程）
//     */
//    private void setPostDTOAssociations(List<PostDTO> postDTOList) {
//        if (postDTOList == null || postDTOList.isEmpty()) {
//            return;
//        }
//
//        // 批量加载用户信息
//        List<Long> userIds = Utils.getIds(postDTOList, dto -> ((PostDTO) dto).getCreatorId());
//        Map<Long, UserBriefDTO> userMap = userService.getUserMap(userIds);
//
//        // 批量加载节点信息
//        List<Long> nodeIds = Utils.getIds(postDTOList, dto -> ((PostDTO) dto).getNodeId());
//        Map<Long, NodeWithCourseDTO> nodeMap = nodeService.getNodeMap(nodeIds);
//
//        // 设置关联信息
//        for (PostDTO postDTO : postDTOList) {
//            postDTO.setCreator(userMap.get(postDTO.getCreatorId()));
//            postDTO.setNode(nodeMap.get(postDTO.getNodeId()));
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 为PostDTOV2列表设置完整的关联信息（用户、节点、课程、投票状态）
//     */
//    private void setPostDTOV2Associations(List<PostDTO> postDTOList, Long currentUserId) {
//        if (postDTOList == null || postDTOList.isEmpty()) {
//            return;
//        }
//
//        // 批量加载用户信息
//        List<Long> userIds = postDTOList.stream().map(PostDTO::getCreatorId).distinct().collect(Collectors.toList());
//        Map<Long, UserDO> userMap = new HashMap<>();
//        if (!userIds.isEmpty()) {
//            List<UserDO> userList = userDataService.getByIds(userIds);
//            userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, user -> user));
//        }
//
//        // 批量加载节点信息
//        List<Long> nodeIds = postDTOList.stream().map(PostDTO::getNodeId).distinct().collect(Collectors.toList());
//        Map<Long, NodeDO> nodeMap = new HashMap<>();
//        if (!nodeIds.isEmpty()) {
//            List<NodeDO> nodeList = nodeDataService.getByIds(nodeIds);
//            nodeMap = nodeList.stream().collect(Collectors.toMap(NodeDO::getId, node -> node));
//        }
//
//        // 批量加载投票状态
//        Map<Long, Integer> voteTypes = new HashMap<>();
//        if (currentUserId != null) {
//            List<Long> postIds = postDTOList.stream().map(PostDTO::getId).collect(Collectors.toList());
//            voteTypes = loadVoteTypes(currentUserId, postIds);
//        }
//
//        // 设置关联信息
//        for (PostDTO postDTO : postDTOList) {
//            // 设置用户信息
//            postDTO.setCreator(userConverter.toBriefDTO(userMap.get(postDTO.getCreatorId())));
//
//            // 设置节点信息
//            NodeDO nodeDO = nodeMap.get(postDTO.getNodeId());
//            if (nodeDO != null) {
//                // 使用 NodeService 的转换方法，会自动填充 course 信息
//                NodeWithCourseDTO nodeDTO = nodeService.toWithCourseDTO(nodeDO);
//                postDTO.setNode(nodeDTO);
//            }
//
//            // 设置投票状态
//            if (voteTypes.containsKey(postDTO.getId())) {
//                postDTO.setVoteType(voteTypes.get(postDTO.getId()));
//            }
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:35):
//    /**
//     * 处理投票状态
//     */
//    private Map<Long, Integer> loadVoteTypes(Long userId, List<Long> postIds) {
//        if (userId == null || postIds == null || postIds.isEmpty()) {
//            return new HashMap<>();
//        }
//
//        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, postIds, ContentType.post.value());
//        Map<Long, Integer> types = new HashMap<>();
//        for (UpvoteDO upvote : upvotes) {
//            types.put(upvote.getObjectId(), upvote.getType());
//        }
//        return types;
//    }
// --注释掉检查 STOP (2025/12/10 11:35)

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 解析JSON字符串到章节信息列表
//     * 格式：[{"章节1": "描述1"}, {"章节2": "描述2"}, {"章节3": "描述3"}]
//     */
//    private List<Utils.Pair<String, String>> parseJsonToChapterInfoList(String jsonContent) {
//        try {
//            List<Map<String, String>> chapterMaps = objectMapper.readValue(jsonContent, new TypeReference<>() {});
//            return chapterMaps.stream().map(chapterMap -> {
//                if (chapterMap.size() != 1) {
//                    throw ErrorCode.INVALID_PARAMETER.exception("每个章节对象必须包含且仅包含一个键值对");
//                }
//                Map.Entry<String, String> entry = chapterMap.entrySet().iterator().next();
//                return new Utils.Pair<>(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
//            }).collect(Collectors.toList());
//        } catch (JsonProcessingException e) {
//            throw ErrorCode.INVALID_PARAMETER.exception("目录内容格式错误，请使用正确的JSON格式");
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 批量处理内容类型帖子的ID转名称
//     */
//    private void processContentPostsIdToName(List<PostDO> postings) {
//        if (postings == null || postings.isEmpty()) {
//            return;
//        }
//
//        // 收集所有内容ID
//        List<Long> allContentIds = postings.stream()
//                .filter(post -> post.getType() != PostType.article.value())
//                .map(PostDO::getContent)
//                .filter(content -> content != null && !content.isEmpty())
//                .flatMap(content -> Arrays.stream(content.split(","))
//                        .map(String::trim)
//                        .filter(s -> !s.isEmpty())
//                        .map(Long::parseLong))
//                .distinct()
//                .collect(Collectors.toList());
//
//        if (allContentIds.isEmpty()) {
//            return;
//        }
//
//        // 批量查询节点信息
//        List<NodeDO> nodeList = nodeDataService.getByIds(allContentIds);
//        Map<Long, NodeSummaryDTO> nodeDTOMap = nodeList.stream()
//                .map(nodeDO -> nodeConverter.toSummaryDTO(nodeDO))
//                .collect(Collectors.toMap(NodeSummaryDTO::getId, node -> node));
//
//        // 为每个帖子转换内容ID为名称
//        for (PostDO postDO : postings) {
//            if (postDO.getType() == PostType.article.value() ||
//                postDO.getContent() == null || postDO.getContent().isEmpty()) {
//                continue;
//            }
//
//            String[] contentIds = postDO.getContent().split(",");
//            StringBuilder newContent = new StringBuilder();
//
//            for (int i = 0; i < contentIds.length; i++) {
//                try {
//                    long nodeId = Long.parseLong(contentIds[i].trim());
//                    NodeSummaryDTO nodeDTO = nodeDTOMap.get(nodeId);
//                    if (nodeDTO != null) {
//                        if (i > 0) {
//                            newContent.append(",");
//                        }
//                        newContent.append(nodeDTO.getName());
//                    }
//                } catch (NumberFormatException e) {
//                    log.warn("Failed to parse content ID: {}", contentIds[i], e);
//                }
//            }
//
//            postDO.setContent(newContent.toString());
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:17)


 // ========== 验证方法 ==========

    /**
     * 验证并获取帖子（委托给 DomainService）
     */
    public PostDO validateAndGetPost(Long postId) {
        return domainService.validateAndGet(postId);
    }

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 验证用户ID
//     */
//    private void validateUserId(Long userId) {
//        if (userId == null || userId <= 0) {
//            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

// --注释掉检查 START (2025/12/10 11:18):
//    /**
//     * 验证节点ID
//     */
//    private void validateNodeId(Long nodeId) {
//        if (nodeId == null || nodeId <= 0) {
//            throw ErrorCode.POSTING_INVALID_PARAMETER.exception();
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:18)

    /**
     * 从HTML内容中提取图片URL并标记为使用中
     *
     * @param content HTML内容
     * @param refType 引用类型（post/comment等）
     * @param refId 引用ID（帖子ID、评论ID等）
     */
    private void markImagesAsUsed(String content, String refType, Long refId) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        try {
            List<String> imageUrls = extractImageUrls(content);
            if (!imageUrls.isEmpty()) {
                MarkImageUsedRequest request = new MarkImageUsedRequest();
                request.setFileUrls(imageUrls);
                request.setRefType(refType);
                request.setRefId(refId);
                imageUploadService.markAsUsed(request);
                log.info("标记 {} 张图片为使用中，refType={}, refId={}", imageUrls.size(), refType, refId);
            }
        } catch (Exception e) {
            // 标记失败不应影响主流程
            log.error("标记图片失败，refType={}, refId={}", refType, refId, e);
        }
    }

    /**
     * 从HTML内容中提取所有图片URL
     *
     * @param html HTML内容
     * @return 图片URL列表
     */
    private List<String> extractImageUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.isEmpty()) {
            return urls;
        }

        // 使用正则提取 <img src="..."> 标签
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");
        java.util.regex.Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String url = matcher.group(1);
            if (url != null && !url.trim().isEmpty()) {
                urls.add(url);
            }
        }

        return urls;
    }
}
