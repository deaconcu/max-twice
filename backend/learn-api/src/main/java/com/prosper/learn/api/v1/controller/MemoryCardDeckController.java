package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.domain.service.business.MemoryCardDeckService;
import com.prosper.learn.dto.request.CreateDeckRequest;
import com.prosper.learn.dto.request.UpdateDeckRequest;
import com.prosper.learn.dto.response.DeckDetailDTO;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.dto.response.MemoryCardDeckDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
     */
    @GetMapping("/posts/{postId}/decks")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getPostPublicDecks(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(defaultValue = "10") @Positive(message = "限制数量必须大于0") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getPostPublicDecks(
            postId, sortBy, sortOrder, lastScore, lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 需求2: 获取帖子创建者提交的卡片组 - 最新创建，limit通常为1
     */
    @GetMapping("/posts/{postId}/creator-deck")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getPostCreatorDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(defaultValue = "1") @Positive(message = "限制数量必须大于0") Integer limit,
            @CurrentUser UserDO currentUser) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getPostCreatorDeck(
            postId, sortBy, sortOrder, lastScore, lastId, limit, currentUser.getId());

        return ApiResponse.success(result);
    }

    /**
     * 需求3: 获取用户自己在指定帖子下提交的卡片组 - 最新创建，limit通常为1
     */
    @GetMapping("/posts/{postId}/my-deck")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getMyPostDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空")
            @Positive(message = "帖子ID必须大于0")
            Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(defaultValue = "1") @Positive(message = "限制数量必须大于0") Integer limit,
            @CurrentUser UserDO currentUser) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getMyPostDeck(
            postId, currentUser.getId(), sortBy, sortOrder, lastScore, lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 获取当前用户自己的所有卡片组（所有状态）
     * GET /api/v1/memory/users/me/memory-decks
     * 按ID逆序排序
     */
    @GetMapping("/users/me/memory-decks")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getCurrentUserAllDecks(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit,
            @CurrentUser UserDO currentUser) {

        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getUserDecks(
                currentUser.getId(), currentUser.getId(), lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 获取指定用户的卡片组（所有状态）
     * GET /api/v1/memory/users/{userId}/memory-decks
     * 按ID逆序排序
     */
    @GetMapping("/users/{userId}/memory-decks")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getUserDecks(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit,
            @CurrentUser UserDO currentUser) {

        if (limit > 50) {
            limit = 50;
        }

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getUserDecks(
            userId, currentUser.getId(), lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组审核列表 - 包含卡片详情
     */
    @GetMapping("/decks/review")
    @SaCheckLogin
    public ApiResponse<KeysetPageResponse<DeckDetailDTO>> getDecksForReview(
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "创建者ID必须大于0") Long creatorId,
            @RequestParam(required = false) @Min(value = 0, message = "状态不能小于0") Integer state,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @CurrentUser UserDO currentUser) {

        KeysetPageResponse<DeckDetailDTO> result =
                deckService.getDecksForReview(postId, creatorId, state, lastId, currentUser.getId());

        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组详情
     */
    @GetMapping("/decks/{deckId}")
    @SaCheckLogin
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
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getDecksByNode(
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

        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getDecksByNode(
            nodeId, lastScore, lastId, limit, currentUser.getId());

        return ApiResponse.success(result);
    }

    /**
     * 创建卡片组
     */
    @PostMapping("/decks")
    @SaCheckLogin
    public ApiResponse<MemoryCardDeckDTO> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {
        MemoryCardDeckDTO result = deckService.createDeck(currentUser.getId(), request);
        return ApiResponse.success(result);
    }

    /**
     * 更新卡片组
     */
    @PutMapping("/decks/{deckId}")
    @SaCheckLogin
    public ApiResponse<MemoryCardDeckDTO> updateDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @Valid @RequestBody UpdateDeckRequest request,
            @CurrentUser UserDO currentUser) {

        request.setId(deckId);

        MemoryCardDeckDTO result = deckService.updateDeck(currentUser.getId(), request);
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组更新差异
     */
    @GetMapping("/decks/{deckId}/diff")
    @SaCheckLogin
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
    public ApiResponse<MemoryCardDeckDTO> replaceAllCards(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {

        MemoryCardDeckDTO result = deckService.replaceAllCards(currentUser.getId(), deckId, request);
        return ApiResponse.success(result);
    }

    /**
     * AI生成记忆卡片组
     */
    @PostMapping("/decks/{postId}/ai-generate")
    @SaCheckLogin
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
    public ApiResponse<Void> deleteDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long id,
            @CurrentUser UserDO currentUser) {
        deckService.deleteDeck(id, currentUser.getId());
        return ApiResponse.success();
    }

}