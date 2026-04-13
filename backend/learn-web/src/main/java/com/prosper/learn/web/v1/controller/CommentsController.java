package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreateCommentRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.comment.CommentBasicDTO;
import com.prosper.learn.application.dto.response.comment.CommentContextDTO;
import com.prosper.learn.application.dto.response.comment.CommentDetailDTO;
import com.prosper.learn.application.dto.response.comment.CommentWithRepliesDTO;
import com.prosper.learn.application.service.CommentService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
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
public class CommentsController {

    private final CommentService commentService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    @SaCheckLogin
    @RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<CommentDetailDTO>> getCommentReplies(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "lastId必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<CommentDetailDTO> replies = commentService.getCommentReplies(id, lastScore, lastId, currentUser);
        return ApiResponse.success(replies);
    }

    /**
     * 获取评论上下文
     * 根据评论ID获取该评论及其前后评论，用于从外部链接跳转到特定评论
     * 如果目标评论是子评论，返回其父评论ID供前端重定向
     */
    @GetMapping("/comments/{id}/context")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CommentContextDTO> getCommentContext(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {

        CommentContextDTO context = commentService.getCommentContext(id, currentUser);
        return ApiResponse.success(context);
    }

    /**
     * 获取评论基本信息
     * 根据评论ID返回其所属对象类型和ID，用于前端跳转到对应页面
     */
    @GetMapping("/comments/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CommentBasicDTO> getCommentBasic(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {

        CommentBasicDTO basic = commentService.getCommentBasic(id, currentUser.getId());
        return ApiResponse.success(basic);
    }
}