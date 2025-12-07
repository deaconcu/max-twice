package com.prosper.learn.web.v1.controller.admin;

import com.prosper.learn.application.dto.request.OperateRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.deck.DeckWithCreatorDTO;
import com.prosper.learn.application.service.MemoryCardDeckService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.OperationLog;
import com.prosper.learn.web.v1.annotation.RequireRole;
import com.prosper.learn.web.v1.dto.ApiResponse;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.prosper.learn.shared.domain.Enums.*;

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
    public ApiResponse<KeysetPageResponse<DeckWithCreatorDTO>> getAdminDecks(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "限制数量必须大于0") Integer limit) {

        KeysetPageResponse<DeckWithCreatorDTO> response = deckService.getDecksForAdmin(state, postId, creatorId, lastId, limit);
        return ApiResponse.success(response);
    }

    /**
     * 管理后台：批准卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/approve
     */
    @PostMapping("/decks/{deckId}/approve")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "审核通过卡片组",
        level = OperationLevel.MEDIUM,
        targetType = "MemoryCardDeck",
        targetId = "#deckId"
    )
    public ApiResponse<Void> approveDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @CurrentUser UserDO currentUser) {

        deckService.approve(deckId, currentUser.getId());
        return ApiResponse.success();
    }

    /**
     * 管理后台：拒绝卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/reject
     */
    @PostMapping("/decks/{deckId}/reject")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "审核拒绝卡片组",
        level = OperationLevel.MEDIUM,
        targetType = "MemoryCardDeck",
        targetId = "#deckId",
        reason = "#request.reason"
    )
    public ApiResponse<Void> rejectDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestBody(required = false) OperateRequest request,
            @CurrentUser UserDO currentUser) {

        String reason = (request != null) ? request.getReason() : "";
        deckService.reject(deckId, currentUser.getId(), reason);
        return ApiResponse.success();
    }

    /**
     * 管理后台：屏蔽卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/ban
     */
    @PostMapping("/decks/{deckId}/ban")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "屏蔽卡片组",
        level = OperationLevel.MEDIUM,
        targetType = "MemoryCardDeck",
        targetId = "#deckId",
        reason = "#request.reason"
    )
    public ApiResponse<Void> banDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestBody(required = false) OperateRequest request,
            @CurrentUser UserDO currentUser) {

        String reason = (request != null) ? request.getReason() : "";
        deckService.ban(deckId, currentUser.getId(), reason);
        return ApiResponse.success();
    }

    /**
     * 管理后台：恢复卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/restore
     */
    @PostMapping("/decks/{deckId}/restore")
    @RequireRole(UserRole.MODERATOR)
    @OperationLog(
        module = "内容管理",
        type = "恢复卡片组",
        level = OperationLevel.MEDIUM,
        targetType = "MemoryCardDeck",
        targetId = "#deckId"
    )
    public ApiResponse<Void> restoreDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @CurrentUser UserDO currentUser) {

        deckService.restoreDeck(deckId, currentUser.getId());
        return ApiResponse.success();
    }
}
