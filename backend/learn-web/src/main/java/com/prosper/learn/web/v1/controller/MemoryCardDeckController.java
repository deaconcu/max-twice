package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.request.UpdateDeckRequest;
import com.prosper.learn.application.dto.response.deck.DeckDetailDTO;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.deck.DeckWithVoteDTO;
import com.prosper.learn.application.service.MemoryCardDeckService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 记忆卡片组控制器
 */
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MemoryCardDeckController {

    private final MemoryCardDeckService deckService;

    /**
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态，固定每页20条
     * sortBy=score: 按分数降序（最高分在前）
     * sortBy=createdAt: 按ID降序（最新的在前）
     */
    @GetMapping("/posts/{postId}/decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getPostPublicDecks(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getPostPublicDecks(
            postId, sortBy, lastScore, lastId, 20, currentUser != null ? currentUser.getId() : null);

        return ApiResponse.success(result);
    }

    /**
     * 需求2: 获取帖子创建者提交的卡片组 - 固定每页20条
     * 固定按ID降序（最新的在前）
     */
    @GetMapping("/posts/{postId}/creator-deck")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getPostCreatorDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long postId,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getPostCreatorDeck(
            postId, lastId, 20, currentUser.getId());

        return ApiResponse.success(result);
    }

    /**
     * 需求3: 获取用户自己在指定帖子下提交的卡片组 - 固定每页20条
     * sortBy=score: 按分数降序（最高分在前）
     * sortBy=createdAt: 按ID降序（最新的在前）
     */
    @GetMapping("/posts/{postId}/my-deck")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getMyPostDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getMyPostDeck(
            postId, currentUser.getId(), sortBy, lastScore, lastId, 20);

        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户自己的所有卡片组（所有状态）
     * GET /api/v1/memory/users/me/memory-decks
     * 按ID逆序排序，固定每页20条
     */
    @GetMapping("/users/me/memory-decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getCurrentUserAllDecks(
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getUserDecks(
                currentUser.getId(), currentUser.getId(), lastId, 20);

        return ApiResponse.success(result);
    }

    /**
     * 获取指定用户的卡片组（所有状态）
     * GET /api/v1/memory/users/{userId}/memory-decks
     * 按ID逆序排序，固定每页20条
     */
    @GetMapping("/users/{userId}/memory-decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getUserDecks(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getUserDecks(
            userId, currentUser.getId(), lastId, 20);

        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组详情
     */
    @GetMapping("/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<DeckDetailDTO> getDeckDetail(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @CurrentUser UserDO currentUser) {
        DeckDetailDTO result = deckService.getDeckDetail(deckId, currentUser.getId());
        return ApiResponse.success(result);
    }

    /**
     * 获取指定节点下的卡片组列表
     */
    @GetMapping("/decks/node/{nodeId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<DeckWithVoteDTO>> getDecksByNode(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit,
            @CurrentUser UserDO currentUser) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<DeckWithVoteDTO> result = deckService.getDecksByNode(
            nodeId, lastScore, lastId, limit, currentUser.getId());

        return ApiResponse.success(result);
    }

    /**
     * 创建卡片组
     */
    @PostMapping("/decks")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {
        deckService.createDeck(currentUser.getId(), request);
        return ApiResponse.success();
    }

    /**
     * 更新卡片组
     */
    @PutMapping("/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @Valid @RequestBody UpdateDeckRequest request,
            @CurrentUser UserDO currentUser) {

        deckService.updateDeck(currentUser.getId(), deckId, request.getDescription());
        return ApiResponse.success();
    }

    /**
     * 获取卡片组更新差异
     */
    @GetMapping("/decks/{deckId}/diff")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> getDeckDiff(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @RequestParam(required = false) @Positive(message = "版本号必须大于0") Integer userCurrentVersion,
            @CurrentUser UserDO currentUser) {
        Object diffResult = deckService.getDeckDiff(deckId, userCurrentVersion, currentUser.getId());
        return ApiResponse.success(diffResult);
    }

    /**
     * 接受卡片组更新
     */
    @PostMapping("/decks/{deckId}/accept-changes")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> acceptDeckChanges(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @RequestBody java.util.List<Long> cardIds,
            @CurrentUser UserDO currentUser) {
        deckService.acceptDeckChanges(deckId, cardIds, currentUser.getId());
        return ApiResponse.success();
    }

    /**
     * 整体替换卡片组中的所有卡片
     */
    @PutMapping("/decks/{deckId}/cards")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> replaceAllCards(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {

        deckService.replaceAllCards(currentUser.getId(), deckId, request);
        return ApiResponse.success();
    }

    /**
     * AI生成记忆卡片组
     */
    @PostMapping("/decks/{postId}/ai-generate")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> createAIDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long postId,
            @CurrentUser UserDO currentUser) {
        deckService.createAIDeck(currentUser.getId(), postId);
        return ApiResponse.success();
    }

    /**
     * 删除卡片组（软删除）
     * DELETE /api/v1/memory/decks/{id}
     */
    @DeleteMapping("/decks/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> deleteDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        deckService.deleteDeck(id, currentUser.getId());
        return ApiResponse.success();
    }

}