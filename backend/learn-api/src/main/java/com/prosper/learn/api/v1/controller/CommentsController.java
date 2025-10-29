package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.CommentService;
import com.prosper.learn.dto.request.CreateCommentRequest;
import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理接口
 * 从CommentClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class CommentsController {

    private final CommentService commentService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    @SaCheckLogin
    public ApiResponse<CommentDTO> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserDO currentUser) {
        CommentDTO comment = commentService.createComment(request, currentUser);
        return ApiResponse.success(comment);
    }

    /**
     * 获取对象评论
     * 映射: GET /comment → GET /api/v1/comments?objectId=123&type=1&offsetId=0
     * TODO 要改成按score排序
     */
    @GetMapping("/comments")
    @SaCheckLogin
    public ApiResponse<List<CommentDTO>> getCommentsByObject(
            @RequestParam @NotNull(message = "对象ID不能为空") @Positive(message = "对象ID必须大于0") Long objectId,
            // TODO @Positive(message = "对象类型必须大于0")
            @RequestParam @NotNull(message = "对象类型不能为空") Integer objectType,
            @RequestParam @NotNull(message = "偏移ID不能为空") @Min(value = 0, message = "偏移ID不能小于0") Long offsetId,
            @CurrentUser UserDO currentUser) {

        List<CommentDTO> comments = commentService.getCommentsByObject(objectId, objectType, offsetId, currentUser);
        return ApiResponse.success(comments);
    }

    /**
     * 获取评论回复
     * 映射: GET /comment/{id}/reply → GET /api/v1/comments/{id}/replies?offsetId=0
     * TODO 要改成按score排序
     */
    @GetMapping("/comments/{id}/replies")
    @SaCheckLogin
    public ApiResponse<List<CommentDTO>> getCommentReplies(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @RequestParam @NotNull(message = "偏移ID不能为空") @Min(value = 0, message = "偏移ID不能小于0") Long offsetId,
            @CurrentUser UserDO currentUser) {

        List<CommentDTO> replies = commentService.getCommentReplies(id, offsetId, currentUser);
        return ApiResponse.success(replies);
    }
}