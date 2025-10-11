package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.CommentService;
import com.prosper.learn.dto.request.CreateCommentRequest;
import com.prosper.learn.dto.response.CommentDTO;
import jakarta.validation.Valid;
import com.prosper.learn.persistence.dataobject.CommentDO;
import lombok.RequiredArgsConstructor;
import com.prosper.learn.api.v1.annotation.JsonParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理接口
 * 从CommentClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentService commentService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    public ApiResponse<CommentDO> createComment(@Valid @RequestBody CreateCommentRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        CommentDO comment = commentService.createComment(request, userId);
        return ApiResponse.success(comment);
    }

    /**
     * 获取对象评论
     * 映射: GET /comment → GET /api/v1/comments?objectId=123&type=1&offsetId=0
     */
    @GetMapping("/comments")
    public ApiResponse<List<CommentDTO>> getCommentsByObject(
            @RequestParam Long objectId, 
            @RequestParam Integer objectType,
            @RequestParam Long offsetId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<CommentDTO> comments = commentService.getCommentsByObject(objectId, objectType, offsetId, userId);
        return ApiResponse.success(comments);
    }

    /**
     * 获取评论回复
     * 映射: GET /comment/{id}/reply → GET /api/v1/comments/{id}/replies?offsetId=0
     */
    @GetMapping("/comments/{id}/replies")
    public ApiResponse<List<CommentDTO>> getCommentReplies(
            @PathVariable Long id, 
            @RequestParam Long offsetId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<CommentDTO> replies = commentService.getCommentReplies(id, offsetId, userId);
        return ApiResponse.success(replies);
    }

    /**
     * 获取待审核评论
     * 映射: GET /comment/censor → GET /api/v1/admin/comments/pending
     */
    @GetMapping("/admin/comments/pending")
    public ApiResponse<List<CommentDTO>> getPendingComments() {
        List<CommentDTO> pendingComments = commentService.getPendingComments();
        return ApiResponse.success(pendingComments);
    }

    /**
     * 审核评论
     * 映射: PUT /comment → PUT /api/v1/admin/comments/{id}/approve
     */
    @PutMapping("/admin/comments/{id}/approve")
    public ApiResponse<CommentDTO> approveComment(
            @PathVariable Long id, 
            @JsonParam("approve") Boolean approve) {
        CommentDTO result = commentService.approveComment(id, approve);
        return ApiResponse.success(result);
    }
}