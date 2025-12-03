package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.business.service.application.CommentService;
import com.prosper.learn.dto.request.CreateCommentRequest;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.dto.response.comment.CommentDetailDTO;
import com.prosper.learn.dto.response.comment.CommentWithRepliesDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 评论管理接口
 * 从CommentClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class CommentsController {

    private final CommentService commentService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    @SaCheckLogin
    public ApiResponse<CommentDetailDTO> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserDO currentUser) {
        CommentDetailDTO comment = commentService.createComment(request, currentUser);
        return ApiResponse.success(comment);
    }

    /**
     * 获取对象评论
     * 映射: GET /comment → GET /api/v1/comments?objectId=123&type=1&lastScore=10.5&lastId=100
     * 按 score 降序、id 降序排序（score 高的在前，score 相同时 id 大的在前）
     */
    @GetMapping("/comments")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<CommentWithRepliesDTO>> getCommentsByObject(
            @RequestParam @NotNull(message = "对象ID不能为空") @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam @NotNull(message = "对象类型不能为空") @Positive(message = "对象类型必须大于0") Integer objectType,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "lastId必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<CommentWithRepliesDTO> comments = commentService.getCommentsByObject(objectId, objectType, lastScore, lastId, currentUser);
        return ApiResponse.success(comments);
    }

    /**
     * 获取评论回复
     * 映射: GET /comment/{id}/reply → GET /api/v1/comments/{id}/replies?lastScore=10.5&lastId=100
     * 按 score 降序、id 降序排序（score 高的在前，score 相同时 id 大的在前）
     */
    @GetMapping("/comments/{id}/replies")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<CommentDetailDTO>> getCommentReplies(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "lastId必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<CommentDetailDTO> replies = commentService.getCommentReplies(id, lastScore, lastId, currentUser);
        return ApiResponse.success(replies);
    }
}