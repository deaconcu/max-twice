package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.MemoryCardDeckService;
import com.prosper.learn.dto.request.CreateDeckRequest;
import com.prosper.learn.dto.request.UpdateDeckRequest;
import com.prosper.learn.dto.response.DeckDetailDTO;
import com.prosper.learn.dto.response.KeysetPageResponse;
import com.prosper.learn.dto.response.MemoryCardDeckDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 记忆卡片组控制器
 */
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Slf4j
public class MemoryCardDeckController {

    private final MemoryCardDeckService deckService;

    /**
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
     */
    @GetMapping("/posts/{postId}/decks")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getPostPublicDecks(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit) {

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
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getPostCreatorDeck(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "1") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getPostCreatorDeck(
            postId, sortBy, sortOrder, lastScore, lastId, limit, userId);

        return ApiResponse.success(result);
    }

    /**
     * 需求3: 获取用户自己在指定帖子下提交的卡片组 - 最新创建，limit通常为1
     */
    @GetMapping("/posts/{postId}/my-deck")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getMyPostDeck(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "1") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getMyPostDeck(
            postId, userId, sortBy, sortOrder, lastScore, lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 需求4: 获取用户自己提交的所有卡片组 - keyset分页，全部状态
     */
    @GetMapping("/decks/my")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getMyDecks(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getMyAllDecks(
            userId, sortBy, sortOrder, lastScore, lastId, limit);

        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组审核列表 - 包含卡片详情
     */
    @GetMapping("/decks/review")
    public ApiResponse<KeysetPageResponse<DeckDetailDTO>> getDecksForReview(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<DeckDetailDTO> result = deckService.getDecksForReview(
            postId, creatorId, state, sortBy, sortOrder, lastScore, lastId, limit, userId);
        
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片组详情
     */
    @GetMapping("/decks/{deckId}")
    public ApiResponse<DeckDetailDTO> getDeckDetail(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        DeckDetailDTO result = deckService.getDeckDetail(deckId, userId);
        return ApiResponse.success(result);
    }

    /**
     * 获取指定节点下的卡片组列表
     */
    @GetMapping("/decks/node/{nodeId}")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getDecksByNode(
            @PathVariable Long nodeId,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") Integer limit) {

        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getDecksByNode(
            nodeId, lastScore, lastId, limit, userId);

        return ApiResponse.success(result);
    }

    /**
     * 创建卡片组
     */
    @PostMapping("/decks")
    public ApiResponse<MemoryCardDeckDTO> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        MemoryCardDeckDTO result = deckService.createDeck(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 更新卡片组
     */
    @PutMapping("/decks/{deckId}")
    public ApiResponse<MemoryCardDeckDTO> updateDeck(
            @PathVariable Long deckId,
            @Valid @RequestBody UpdateDeckRequest request) {
        
        long userId = StpUtil.getLoginIdAsLong();
        request.setId(deckId);
        
        MemoryCardDeckDTO result = deckService.updateDeck(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 审核通过卡片组
     */
    @PostMapping("/decks/{deckId}/approve")
    public ApiResponse<Void> approveDeck(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.approveDeck(deckId, userId);
        return ApiResponse.success();
    }

    /**
     * 废弃卡片组（审核拒绝或屏蔽）
     */
    @PostMapping("/decks/{deckId}/discard")
    public ApiResponse<Void> discardDeck(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.discardDeck(deckId, userId);
        return ApiResponse.success();
    }

    /**
     * 恢复卡片组
     */
    @PostMapping("/decks/{deckId}/restore")
    public ApiResponse<Void> restoreDeck(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.restoreDeck(deckId, userId);
        return ApiResponse.success();
    }

    /**
     * 获取卡片组更新差异
     */
    @GetMapping("/decks/{deckId}/diff")
    public ApiResponse<Object> getDeckDiff(
            @PathVariable Long deckId,
            @RequestParam(required = false) Integer userCurrentVersion) {
        Long userId = StpUtil.getLoginIdAsLong();
        Object diffResult = deckService.getDeckDiff(deckId, userCurrentVersion, userId);
        return ApiResponse.success(diffResult);
    }

    /**
     * 接受卡片组更新
     */
    @PostMapping("/decks/{deckId}/accept-changes")
    public ApiResponse<Void> acceptDeckChanges(
            @PathVariable Long deckId,
            @RequestBody java.util.List<Long> cardIds) {
        Long userId = StpUtil.getLoginIdAsLong();
        deckService.acceptDeckChanges(deckId, cardIds, userId);
        return ApiResponse.success();
    }

    /**
     * 整体替换卡片组中的所有卡片
     */
    @PutMapping("/decks/{deckId}/cards")
    public ApiResponse<MemoryCardDeckDTO> replaceAllCards(
            @PathVariable Long deckId,
            @Valid @RequestBody CreateDeckRequest request) {

        long userId = StpUtil.getLoginIdAsLong();
        MemoryCardDeckDTO result = deckService.replaceAllCards(userId, deckId, request);
        return ApiResponse.success(result);
    }

    /**
     * AI生成记忆卡片组
     */
    @PostMapping("/decks/{postId}/ai-generate")
    public ApiResponse<Void> createAIDeck(@PathVariable Long postId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.createAIDeck(userId, postId);
        return ApiResponse.success();
    }

}