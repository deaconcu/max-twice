package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.CommentService;
import com.prosper.learn.dto.request.CreateCommentRequest;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.CommentDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class CommentsController {

    private final CommentService commentService;

    /**
     * 创建评论
     * 映射: POST /comment → POST /api/v1/comments
     */
    @PostMapping("/comments")
    public ApiResponse<CommentDTO> createComment(@Valid @RequestBody CreateCommentRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        CommentDTO comment = commentService.createComment(request, userId);
        return ApiResponse.success(comment);
    }

    /**
     * 获取对象评论
     * 映射: GET /comment → GET /api/v1/comments?objectId=123&type=1&offsetId=0
     * TODO 要改成按score排序
     */
    @GetMapping("/comments")
    public ApiResponse<List<CommentDTO>> getCommentsByObject(
            @RequestParam @NotNull(message = "对象ID不能为空") @Positive(message = "对象ID必须大于0") Long objectId,
            // TODO @Positive(message = "对象类型必须大于0")
            @RequestParam @NotNull(message = "对象类型不能为空") Integer objectType,
            @RequestParam @NotNull(message = "偏移ID不能为空") @Min(value = 0, message = "偏移ID不能小于0") Long offsetId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<CommentDTO> comments = commentService.getCommentsByObject(objectId, objectType, offsetId, userId);
        return ApiResponse.success(comments);
    }

    /**
     * 获取评论回复
     * 映射: GET /comment/{id}/reply → GET /api/v1/comments/{id}/replies?offsetId=0
     * TODO 要改成按score排序
     */
    @GetMapping("/comments/{id}/replies")
    public ApiResponse<List<CommentDTO>> getCommentReplies(
            @PathVariable @NotNull(message = "评论ID不能为空") @Positive(message = "评论ID必须大于0") Long id,
            @RequestParam @NotNull(message = "偏移ID不能为空") @Min(value = 0, message = "偏移ID不能小于0") Long offsetId) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        List<CommentDTO> replies = commentService.getCommentReplies(id, offsetId, userId);
        return ApiResponse.success(replies);
    }

    /**
     * 获取指定状态的评论（分页）
     * 映射: GET /api/v1/admin/comments/{state}?offsetId=0
     * state: pending(待审核), approved(已通过), rejected(已拒绝)
     */
    @GetMapping("/admin/comments/{state}")
    public ApiResponse<List<CommentDTO>> getCommentsByState(
            @PathVariable String state,
            @RequestParam(required = false) @Min(value = 0, message = "偏移ID不能小于0") Long offsetId) {
        Long actualOffsetId = (offsetId == null) ? 0L : offsetId;
        List<CommentDTO> comments = commentService.getCommentsByState(state, actualOffsetId);
        return ApiResponse.success(comments);
    }

    /**
     * 评论审核操作
     * 映射: POST /comment/operate → POST /api/v1/admin/comments/{id}/approve
     */
    @PostMapping("/admin/comments/{id}/approve")
    public ApiResponse<ApprovalResponseDTO> approveComment(
            @PathVariable @NotNull(message = "评论ID不能为空")
            @Positive(message = "评论ID必须大于0") Long id,
            @RequestBody @Valid OperateRequest request) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                commentService.approve(id);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("comment")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                commentService.reject(id, request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("comment")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                commentService.ban(id, request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(id)
                        .objectType("comment")
                        .action("ban")
                        .build();
            }
            default -> throw ErrorCode.INVALID_PARAMETER.exception();
        };

        return ApiResponse.success(response);
    }
}