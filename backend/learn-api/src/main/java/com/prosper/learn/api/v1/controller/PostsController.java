package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.PostService;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.dto.request.UpdatePostRequest;
import com.prosper.learn.dto.response.PostDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 批量获取帖子
     * 映射: GET /postings?ids=1,2,3 → GET /api/v1/posts?ids=1,2,3
     */
    @GetMapping("/posts")
    public ApiResponse<List<PostDTO>> getPosts(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "lastScore", required = false, defaultValue = "0") double lastScore,
            @RequestParam(value = "lastId", required = false, defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastPostingId) {
        
        long currentUserId = StpUtil.getLoginIdAsLong();
        List<PostDTO> posts = postService.getPostsWithUserAndVoteInfo(ids, nodeId, lastScore, lastPostingId, currentUserId);
        return ApiResponse.success(posts);
    }

    /**
     * 创建帖子
     * 映射: POST /posting → POST /api/v1/posts
     */
    @PostMapping("/posts")
    public ApiResponse<Void> createPost(@Valid @RequestBody CreatePostRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        postService.createPost(userId, request);
        return ApiResponse.success();
    }

    /**
     * 修改帖子
     * 映射: PUT /posting → PUT /api/v1/posts/{id}
     */
    @PutMapping("/posts/{id}")
    public ApiResponse<Void> updatePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        postService.updatePost(id, request);
        return ApiResponse.success();
    }

    /**
     * 删除帖子
     * 映射: DELETE /posting → DELETE /api/v1/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id) {
        postService.deletePost(id);
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
     * 根据状态获取帖子列表
     */
    @GetMapping("/admin/posts")
    public ApiResponse<List<PostDTO>> getPostsByState(
            @RequestParam("state") @NotBlank(message = "状态不能为空") String state,
            @RequestParam(value = "lastId", defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(value = "limit", defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit) {
        Enums.ContentState postState;

        switch (state) {
            case "pending":
                postState = Enums.ContentState.SUBMITTED;
                break;
            case "approved":
                postState = Enums.ContentState.APPROVED;
                break;
            case "rejected":
                postState = Enums.ContentState.REJECTED;
                break;
            default:
                postState = Enums.ContentState.SUBMITTED;
        }

        List<PostDTO> posts = postService.getPostsByState(postState, lastId, limit);
        return ApiResponse.success(posts);
    }

    /**
     * 根据节点和用户筛选帖子列表
     */
    @GetMapping("/admin/posts/filter")
    public ApiResponse<List<PostDTO>> getPostsByNodeAndCreator(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastId) {
        List<PostDTO> posts = postService.getPostsByNodeAndCreator(nodeId, creatorId, lastId);
        return ApiResponse.success(posts);
    }

    /**
     * 获取待审核帖子
     * 映射: GET /post/censor → GET /api/v1/admin/posts/pending
     */
    @GetMapping("/admin/posts/pending")
    public ApiResponse<List<PostDTO>> getPendingPosts() {
        List<PostDTO> posts = postService.getPendingPostsList();
        return ApiResponse.success(posts);
    }

    /**
     * 审核帖子
     * 映射: PUT /post → PUT /api/v1/admin/posts/{id}/approve
     */
    @PutMapping("/admin/posts/{id}/approve")
    public ApiResponse<PostDTO> approvePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @RequestParam("approve") @NotNull(message = "审核结果不能为空") Boolean approve) {
        PostDTO post = postService.approvePost(id, approve);
        return ApiResponse.success(post);
    }
}