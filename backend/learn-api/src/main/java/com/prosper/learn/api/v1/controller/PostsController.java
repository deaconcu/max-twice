package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.PostingService;
import com.prosper.learn.dto.request.CreatePostRequest;
import com.prosper.learn.dto.request.UpdatePostRequest;
import com.prosper.learn.dto.response.PostDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.prosper.learn.api.v1.annotation.JsonParam;
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
public class PostsController {

    private final PostingService postingService;

    /**
     * 批量获取帖子
     * 映射: GET /postings?ids=1,2,3 → GET /api/v1/posts?ids=1,2,3
     */
    @GetMapping("/posts")
    public ApiResponse<List<PostDTO>> getPosts(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "nodeId", required = false) Long nodeId,
            @RequestParam(value = "lastScore", required = false, defaultValue = "0") double lastScore,
            @RequestParam(value = "lastId", required = false, defaultValue = "0") Long lastPostingId) {
        
        long currentUserId = StpUtil.getLoginIdAsLong();
        List<PostDTO> posts = postingService.getPostsWithUserAndVoteInfo(ids, nodeId, lastScore, lastPostingId, currentUserId);
        return ApiResponse.success(posts);
    }

    /**
     * 创建帖子
     * 映射: POST /posting → POST /api/v1/posts
     */
    @PostMapping("/posts")
    public ApiResponse<Void> createPost(@Valid @RequestBody CreatePostRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        postingService.createPost(userId, request);
        return ApiResponse.success();
    }

    /**
     * 修改帖子
     * 映射: PUT /posting → PUT /api/v1/posts/{id}
     */
    @PutMapping("/posts/{id}")
    public ApiResponse<Void> updatePost(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
        postingService.updatePost(id, request);
        return ApiResponse.success();
    }

    /**
     * 删除帖子
     * 映射: DELETE /posting → DELETE /api/v1/posts/{id}
     */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        postingService.deletePost(id);
        return ApiResponse.success();
    }

    /**
     * 获取帖子详情
     * 映射: GET /posting/{id} → GET /api/v1/posts/{id}
     */
    @GetMapping("/posts/{id}")
    public ApiResponse<PostDTO> getPost(@PathVariable Long id) {
        PostDTO post = postingService.getPostDetail(id);
        return ApiResponse.success(post);
    }

    /**
     * 获取节点帖子
     * 映射: GET /node/{nodeId}/posting → GET /api/v1/nodes/{nodeId}/posts
     */
    @GetMapping("/nodes/{nodeId}/posts")
    public ApiResponse<List<PostDTO>> getNodePosts(@PathVariable Long nodeId) {
        List<PostDTO> posts = postingService.getNodePostsList(nodeId);
        return ApiResponse.success(posts);
    }

    /**
     * 获取待审核帖子
     * 映射: GET /post/censor → GET /api/v1/admin/posts/pending
     */
    @GetMapping("/admin/posts/pending")
    public ApiResponse<List<PostDTO>> getPendingPosts() {
        List<PostDTO> posts = postingService.getPendingPostsList();
        return ApiResponse.success(posts);
    }

    /**
     * 审核帖子
     * 映射: PUT /post → PUT /api/v1/admin/posts/{id}/approve
     */
    @PutMapping("/admin/posts/{id}/approve")
    public ApiResponse<PostDTO> approvePost(
            @PathVariable Long id, 
            @JsonParam("approve") Boolean approve) {
        PostDTO post = postingService.approvePost(id, approve);
        return ApiResponse.success(post);
    }
}