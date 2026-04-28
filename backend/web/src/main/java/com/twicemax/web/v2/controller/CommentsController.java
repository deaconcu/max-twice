package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateCommentRequest;
import com.twicemax.application.dto.response.comment.CommentBasicDTO;
import com.twicemax.application.dto.response.comment.CommentContextDTO;
import com.twicemax.application.dto.response.comment.CommentDetailDTO;
import com.twicemax.application.dto.response.comment.CommentWithRepliesDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.CommentService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 评论管理接口
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Validated
public class CommentsController {

    private final CommentService commentService;

    @PostMapping
    @SaCheckLogin
    @RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CommentDetailDTO> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @CurrentUser UserDO currentUser) {
        CommentDetailDTO comment = commentService.createComment(request, currentUser);
        return ResponseEntity.status(201).body(comment);
    }

    @GetMapping
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<CommentWithRepliesDTO> getCommentsByObject(
            @RequestParam @NotNull(message = "对象ID不能为空") @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam @NotNull(message = "对象类型不能为空") @Positive(message = "对象类型必须大于0") Integer objectType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Positive(message = "pageSize必须大于0") Integer pageSize,
            @CurrentUser UserDO currentUser) {

        return commentService.getCommentsByObject(objectId, objectType, cursor, pageSize, currentUser);
    }

    @GetMapping("/{id}/replies")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<CommentDetailDTO> getCommentReplies(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Positive(message = "pageSize必须大于0") Integer pageSize,
            @CurrentUser UserDO currentUser) {

        return commentService.getCommentReplies(id, cursor, pageSize, currentUser);
    }

    @GetMapping("/{id}/context")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CommentContextDTO getCommentContext(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {

        return commentService.getCommentContext(id, currentUser);
    }

    @GetMapping("/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CommentBasicDTO getCommentBasic(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {

        return commentService.getCommentBasic(id, currentUser.getId());
    }
}
