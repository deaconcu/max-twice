package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreatePostRequest;
import com.prosper.learn.application.dto.request.UpdatePostRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.node.NodeSearchResultDTO;
import com.prosper.learn.application.dto.response.node.NodeWithCourseDTO;
import com.prosper.learn.application.dto.response.post.PostFullDTO;
import com.prosper.learn.application.dto.response.post.PostSummaryDTO;
import com.prosper.learn.application.dto.response.post.PostWithVoteDTO;
import com.prosper.learn.application.service.NodeService;
import com.prosper.learn.application.service.PostService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 帖子管理接口
 * 从PostClient和AggregateClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PostsController {

    private final PostService postService;
    private final NodeService nodeService;

    /**
     * 批量获取帖子
     * 支持两种查询方式：
     * 1. 按IDs批量查询：传 ids 参数，返回 List
     * 2. 按节点分页查询：传 nodeId + 分页参数，返回 KeysetPageResponse
     *
     * 映射: GET /postings?ids=1,2,3 → GET /api/v1/posts?ids=1,2,3
     */
    @GetMapping("/posts")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<?> getPosts(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "lastScore", required = false) Double lastScore,
            @RequestParam(value = "lastId", required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastPostingId,
            @CurrentUser UserDO currentUser) {

        // 按 IDs 批量查询 - 返回 List
        if (ids != null && !ids.isEmpty()) {
            List<PostWithVoteDTO> posts = postService.getPostsByIds(ids, currentUser.getId());
            return ApiResponse.success(posts);
        }

        // 按节点分页查询 - 返回 KeysetPageResponse
        if (nodeId != null) {
            KeysetPageResponse<PostWithVoteDTO> response = postService.getNodePostsPage(nodeId, lastScore, lastPostingId, currentUser.getId());
            return ApiResponse.success(response);
        }

        // 参数不足
        return ApiResponse.error(400, "必须提供 ids 或 nodeId 参数");
    }

    /**
     * 创建帖子
     * 映射: POST /posting → POST /api/v1/posts
     */
    @PostMapping("/posts")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<PostSummaryDTO> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @CurrentUser UserDO currentUser) {
        PostSummaryDTO postDTO = postService.createPostAndReturn(currentUser, request);
        return ApiResponse.success(postDTO);
    }

    /**
     * 修改帖子
     * 映射: PUT /posting → PUT /api/v1/posts/{id}
     */
    @PutMapping("/posts/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<PostSummaryDTO> updatePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @CurrentUser UserDO currentUser) {
        PostSummaryDTO postDTO = postService.updatePostAndReturn(id, request, currentUser);
        return ApiResponse.success(postDTO);
    }

    /**
     * 删除帖子（软删除）
     * 映射: DELETE /posting → DELETE /api/v1/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> deletePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        postService.deletePost(id, currentUser);
        return ApiResponse.success();
    }

    /**
     * 获取帖子详情
     * 映射: GET /posting/{id} → GET /api/v1/posts/{id}
     */
    @GetMapping("/posts/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<PostSummaryDTO> getPost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id) {
        PostSummaryDTO post = postService.getDTO(id);
        return ApiResponse.success(post);
    }

    /**
     * 获取用户文章或内容（仅已发布）
     * 映射: GET /user/article → GET /api/v1/users/{userId}/posts?type=2
     * 映射: GET /user/contents → GET /api/v1/users/{userId}/posts?type=1
     */
    @GetMapping("/users/{userId}/posts")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<PostFullDTO>> getUserPosts(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        KeysetPageResponse<PostFullDTO> response = postService.getUserPostsWithPagination(
                userId, lastId, postType, Enums.ContentState.PUBLISHED.value());
        return ApiResponse.success(response);
    }

    /**
     * 获取当前登录用户所有状态的文章或目录（用于个人中心内容管理）
     * 包含：待审核、已发布、审核拒绝、已屏蔽
     * GET /api/v1/users/me/posts?lastId=0&type=2
     */
    @GetMapping("/users/me/posts")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<PostFullDTO>> getCurrentUserAllPosts(
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type,
            @CurrentUser UserDO currentUser) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        KeysetPageResponse<PostFullDTO> result = postService.getUserPostsWithPagination(currentUser.getId(), lastId, postType, null);
        return ApiResponse.success(result);
    }

    /**
     * 搜索相似节点
     * GET /api/v1/nodes/search?query=Java基础&topK=10&threshold=0.0
     */
    @GetMapping("/nodes/search")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<NodeSearchResultDTO>> searchSimilarNodes(
            @RequestParam @NotBlank(message = "查询文本不能为空") String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int topK,
            @RequestParam(defaultValue = "0.2") @DecimalMin("0.0") @DecimalMax("1.0") double threshold) {

        List<NodeSearchResultDTO> nodes = nodeService.searchSimilarNodes(query, topK, threshold);
        return ApiResponse.success(nodes);
    }

    /**
     * 检查课程内是否存在同名已发布节点
     * GET /api/v1/nodes/check-duplicate?courseId=1&name=Java基础
     */
    @GetMapping("/nodes/check-duplicate")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Boolean> checkDuplicateNode(
            @RequestParam @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam @NotBlank(message = "节点名称不能为空") String name) {

        boolean isDuplicate = nodeService.checkDuplicateNode(courseId, name);
        return ApiResponse.success(isDuplicate);
    }
}