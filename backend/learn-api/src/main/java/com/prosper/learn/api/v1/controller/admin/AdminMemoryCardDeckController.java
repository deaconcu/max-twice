package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.annotation.OperationLog;
import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.Enums.UserRole;
import com.prosper.learn.domain.service.business.MemoryCardDeckService;
import com.prosper.learn.dto.request.OperateRequest;
import com.prosper.learn.dto.response.ApprovalResponseDTO;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.dto.response.MemoryCardDeckDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆卡片管理后台接口
 */
@RestController
@RequestMapping("/api/v1/admin/memory")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminMemoryCardDeckController {

    private final MemoryCardDeckService deckService;

    /**
     * 管理后台：查询卡片组列表
     * GET /api/v1/admin/memory/decks?state=0&postId=123&creatorId=456&lastId=789&limit=20
     */
    @GetMapping("/decks")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getAdminDecks(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "限制数量必须大于0") Integer limit) {

        KeysetPageResponse<MemoryCardDeckDTO> response = deckService.getDecksForAdmin(state, postId, creatorId, lastId, limit);
        return ApiResponse.success(response);
    }

    /**
     * 管理后台：卡片组审核操作（统一接口）
     * POST /api/v1/admin/memory/decks/{deckId}/approve
     * 支持 approve/reject/ban/restore 操作
     */
    @PostMapping("/decks/{deckId}/approve")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "#request.action == 'APPROVE' ? '审核通过卡片组' : (#request.action == 'REJECT' ? '审核拒绝卡片组' : (#request.action == 'BAN' ? '屏蔽卡片组' : '恢复卡片组'))",
        level = Enums.OperationLevel.MEDIUM,
        targetType = "MemoryCardDeck",
        targetId = "#deckId",
        reason = "#request.reason"
    )
    public ApiResponse<ApprovalResponseDTO> approveDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestBody @Valid OperateRequest request,
            @CurrentUser UserDO currentUser) {

        ApprovalResponseDTO response = switch (request.getAction().toLowerCase()) {
            case "approve" -> {
                deckService.approve(deckId, currentUser.getId());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("批准成功")
                        .objectId(deckId)
                        .objectType("memoryDeck")
                        .action("approve")
                        .build();
            }
            case "reject" -> {
                deckService.reject(deckId, currentUser.getId(), request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("拒绝成功")
                        .objectId(deckId)
                        .objectType("memoryDeck")
                        .action("reject")
                        .build();
            }
            case "ban" -> {
                deckService.ban(deckId, currentUser.getId(), request.getReason());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("封禁成功")
                        .objectId(deckId)
                        .objectType("memoryDeck")
                        .action("ban")
                        .build();
            }
            case "restore" -> {
                deckService.restoreDeck(deckId, currentUser.getId());
                yield ApprovalResponseDTO.builder()
                        .success(true)
                        .message("恢复成功")
                        .objectId(deckId)
                        .objectType("memoryDeck")
                        .action("restore")
                        .build();
            }
            default -> throw new IllegalArgumentException("不支持的操作类型: " + request.getAction());
        };

        return ApiResponse.success(response);
    }
}
