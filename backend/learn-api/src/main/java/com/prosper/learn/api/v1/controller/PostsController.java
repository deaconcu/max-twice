package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.dto.request.UpdatePostRequest;
import com.prosper.learn.dto.response.PostDTO;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.persistence.dataobject.UserDO;
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
@RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class PostsController {

    private final PostService postService;

    /**
     * 批量获取帖子
     * 映射: GET /postings?ids=1,2,3 → GET /api/v1/posts?ids=1,2,3
     */
    @GetMapping("/posts")
    @SaCheckLogin
    public ApiResponse<List<PostDTO>> getPosts(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "lastScore", required = false, defaultValue = "0") double lastScore,
            @RequestParam(value = "lastId", required = false, defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastPostingId,
            @CurrentUser UserDO currentUser) {

        List<PostDTO> posts = postService.getPostsWithUserAndVoteInfo(ids, nodeId, lastScore, lastPostingId, currentUser.getId());
        return ApiResponse.success(posts);
    }

    /**
     * 创建帖子
     * 映射: POST /posting → POST /api/v1/posts
     */
    @PostMapping("/posts")
    @SaCheckLogin
    public ApiResponse<Void> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @CurrentUser UserDO currentUser) {
        postService.createPost(currentUser, request);
        return ApiResponse.success();
    }

    /**
     * 修改帖子
     * 映射: PUT /posting → PUT /api/v1/posts/{id}
     */
    @PutMapping("/posts/{id}")
    @SaCheckLogin
    public ApiResponse<PostDTO> updatePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @CurrentUser UserDO currentUser) {
        PostDTO postDTO = postService.updatePostAndReturn(id, request, currentUser);
        return ApiResponse.success(postDTO);
    }

    /**
     * 删除帖子（软删除）
     * 映射: DELETE /posting → DELETE /api/v1/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    @SaCheckLogin
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
    public ApiResponse<PostDTO> getPost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id) {
        PostDTO post = postService.getDTO(id);
        return ApiResponse.success(post);
    }

    /**
     * 获取节点帖子
     * 映射: GET /node/{nodeId}/posting → GET /api/v1/nodes/{nodeId}/posts
     */
    @GetMapping("/nodes/{nodeId}/posts")
    public ApiResponse<List<PostDTO>> getNodePosts(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId) {
        List<PostDTO> posts = postService.getNodePostsList(nodeId);
        return ApiResponse.success(posts);
    }

    /**
     * 获取用户文章或内容（仅已发布）
     * 映射: GET /user/article → GET /api/v1/users/{userId}/posts?type=2
     * 映射: GET /user/contents → GET /api/v1/users/{userId}/posts?type=1
     */
    @GetMapping("/users/{userId}/posts")
    public ApiResponse<List<PostDTO>> getUserPosts(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        List<PostDTO> posts = postService.getUserPosts(userId, lastId, postType, Enums.ContentState.PUBLISHED.value());
        return ApiResponse.success(posts);
    }

    /**
     * 获取当前登录用户所有状态的文章或目录（用于个人中心内容管理）
     * 包含：待审核、已发布、审核拒绝、已屏蔽
     * GET /api/v1/users/me/posts?lastId=0&type=2
     */
    @GetMapping("/users/me/posts")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<PostDTO>> getCurrentUserAllPosts(
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false, defaultValue = "2") Integer type,
            @CurrentUser UserDO currentUser) {

        Enums.PostType postType = Enums.PostType.getByValue(type);
        if (postType == null) {
            return ApiResponse.error(400, "无效的帖子类型");
        }
        KeysetPageResponse<PostDTO> result = postService.getUserPostsWithPagination(currentUser.getId(), lastId, postType, null);
        return ApiResponse.success(result);
    }
}