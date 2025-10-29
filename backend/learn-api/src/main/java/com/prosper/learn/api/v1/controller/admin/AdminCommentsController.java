package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.CommentService;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminCommentsController {

    private final CommentService commentService;

    /**
     * 获取指定状态的评论（分页）
     * 映射: GET /api/v1/admin/comments/{state}?offsetId=0
     * state: pending(待审核), approved(已通过), rejected(已拒绝)
     */
    @GetMapping("/comments/{state}")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<List<CommentDTO>> getCommentsByState(
            @PathVariable String state,
            @RequestParam(required = false) @Min(value = 0, message = "偏移ID不能小于0") Long offsetId) {
        Long actualOffsetId = (offsetId == null) ? 0L : offsetId;
        List<CommentDTO> comments = commentService.getCommentsByState(state, actualOffsetId);
        return ApiResponse.success(comments);
    }

    /**
     * 根据对象类型、对象ID、创建者和状态筛选评论列表
     */
    @GetMapping("/comments/filter")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<List<CommentDTO>> getCommentsByFilter(
            @RequestParam(value = "objectType", required = false) @Positive(message = "对象类型必须大于0") Integer objectType,
            @RequestParam(value = "objectId", required = false) @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId",  required = false) @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(value = "state", required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state) {
        List<CommentDTO> comments = commentService.getCommentsByFilter(objectType, objectId, creatorId, lastId, state);
        return ApiResponse.success(comments);
    }

    /**
     * 评论审核操作
     * 映射: POST /comment/operate → POST /api/v1/admin/comments/{id}/approve
     */
    @PostMapping("/comments/{id}/approve")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "#request.action == 'APPROVE' ? '审核通过评论' : (#request.action == 'REJECT' ? '审核拒绝评论' : '屏蔽评论')",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Comment",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ApiResponse<ApprovalResponseDTO> approveComment(
            @PathVariable @NotNull(message = "评论ID不能为空")
            @Positive(message = "评论ID必须大于0") Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                commentService.approve(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("comment")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                commentService.reject(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("comment")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                commentService.ban(id, request.getReason(), currentUser);
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
