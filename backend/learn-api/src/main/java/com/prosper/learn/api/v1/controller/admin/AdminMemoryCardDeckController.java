package com.prosper.learn.api.v1.controller.admin;

import com.prosper.learn.api.v1.annotation.RequireRole;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 记忆卡片管理后台接口
 * TODO: 需要注入相应的 Service，并实现以下接口
 */
@RestController
@RequestMapping("/api/v1/admin/memory")
@RequiredArgsConstructor
@Slf4j
@RequireRole(UserRole.ADMIN)
@Validated
public class AdminMemoryCardDeckController {

    // TODO: 注入 MemoryCardDeckService

    /**
     * 管理后台：查询卡片组列表
     * GET /api/v1/admin/memory/decks?state=0&postId=123&creatorId=456&lastId=789
     * TODO: 需要实现 Service 方法支持按状态、postId、creatorId 筛选
     */
    @GetMapping("/decks")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> getAdminDecks(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) @Positive(message = "限制数量必须大于0") Integer limit) {

        // TODO: 调用 Service 方法
        throw new UnsupportedOperationException("待实现：需要实现 MemoryCardDeckService.getDecksForAdmin() 方法");
    }

    /**
     * 管理后台：审核通过卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/approve
     * TODO: 需要实现 Service 方法
     */
    @PostMapping("/decks/{deckId}/approve")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> approveDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId) {

        // TODO: 调用 Service 方法
        throw new UnsupportedOperationException("待实现：需要实现 MemoryCardDeckService.approveDeck() 方法");
    }

    /**
     * 管理后台：拒绝卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/reject
     * TODO: 需要实现 Service 方法
     */
    @PostMapping("/decks/{deckId}/reject")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> rejectDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestParam(required = false) String reason) {

        // TODO: 调用 Service 方法
        throw new UnsupportedOperationException("待实现：需要实现 MemoryCardDeckService.rejectDeck() 方法");
    }

    /**
     * 管理后台：屏蔽卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/ban
     * TODO: 需要实现 Service 方法
     */
    @PostMapping("/decks/{deckId}/ban")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> banDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestParam(required = false) String reason) {

        // TODO: 调用 Service 方法
        throw new UnsupportedOperationException("待实现：需要实现 MemoryCardDeckService.banDeck() 方法");
    }

    /**
     * 管理后台：恢复卡片组
     * POST /api/v1/admin/memory/decks/{deckId}/restore
     * TODO: 需要实现 Service 方法
     */
    @PostMapping("/decks/{deckId}/restore")
    @RequireRole(UserRole.MODERATOR)
    public ApiResponse<Object> restoreDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0") Long deckId) {

        // TODO: 调用 Service 方法
        throw new UnsupportedOperationException("待实现：需要实现 MemoryCardDeckService.restoreDeck() 方法");
    }
}
