package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.business.service.application.PostService;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.post.PostSummaryDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@RequireRole(Enums.UserRole.ADMIN)
@Validated
public class AdminPostsController {

    private final PostService postService;

    /**
     * 根据状态获取帖子列表
     */
    @GetMapping("/posts")
    @RequireRole(Enums.UserRole.MODERATOR)
    public ApiResponse<List<PostSummaryDTO>> getPostsByState(
            @RequestParam("state") @NotBlank(message = "状态不能为空") String state,
            @RequestParam(value = "lastId", defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(value = "limit", defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit) {
        Enums.ContentState postState;

        switch (state) {
            case "pending":
                postState = Enums.ContentState.SUBMITTED;
                break;
            case "approved":
                postState = Enums.ContentState.PUBLISHED;
                break;
            case "rejected":
                postState = Enums.ContentState.REJECTED;
                break;
            case "banned":
                postState = Enums.ContentState.BANNED;
                break;
            default:
                postState = Enums.ContentState.SUBMITTED;
        }

        List<PostSummaryDTO> posts = postService.getPostsByState(postState, lastId, limit);
        return ApiResponse.success(posts);
    }

    /**
     * 根据节点、用户和状态筛选帖子列表
     */
    @GetMapping("/posts/filter")
    @RequireRole(Enums.UserRole.MODERATOR)
    public ApiResponse<List<PostSummaryDTO>> getPostsByNodeAndCreator(
            @RequestParam(value = "nodeId", required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(value = "creatorId", required = false) @Positive(message = "用户ID必须大于0") Long creatorId,
            @RequestParam(value = "lastId", defaultValue = "0") @Min(value = 0, message = "最后ID不能小于0") Long lastId,
            @RequestParam(value = "state", required = false) @Min(value = 0, message = "状态必须大于等于0") Byte state) {
        List<PostSummaryDTO> posts = postService.getPostsByNodeAndCreator(nodeId, creatorId, lastId, state);
        return ApiResponse.success(posts);
    }

    /**
     * 获取待审核帖子
     * 映射: GET /post/censor → GET /api/v1/admin/posts/pending
     */
    @GetMapping("/posts/pending")
    @RequireRole(Enums.UserRole.MODERATOR)
    public ApiResponse<List<PostSummaryDTO>> getPendingPosts() {
        List<PostSummaryDTO> posts = postService.getPendingPostsList();
        return ApiResponse.success(posts);
    }

    /**
     * 帖子审核操作
     * 映射: POST /post/operate → POST /api/v1/admin/posts/{id}/approve
     */
    @PostMapping("/posts/{id}/approve")
    @RequireRole(Enums.UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "#request.action == 'APPROVE' ? '审核通过帖子' : (#request.action == 'REJECT' ? '审核拒绝帖子' : '屏蔽帖子')",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "Post",
        targetId = "#id",
        reason = "#request.reason"
    )
    public ApiResponse<ApprovalResponseDTO> approvePost(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long id,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                postService.approve(id, currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(id)
                        .objectType("post")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                postService.reject(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(id)
                        .objectType("post")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                postService.ban(id, request.getReason(), currentUser);
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(id)
                        .objectType("post")
                        .action("ban")
                        .build();
            }
            default -> throw ErrorCode.INVALID_PARAMETER.exception();
        };

        return ApiResponse.success(response);
    }
}
