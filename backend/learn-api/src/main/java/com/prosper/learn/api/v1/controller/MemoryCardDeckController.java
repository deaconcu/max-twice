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
     * 获取卡片组列表
     */
    @GetMapping("/decks")
    public ApiResponse<KeysetPageResponse<MemoryCardDeckDTO>> getDecks(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) Double lastScore,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        // 限制每页最大数量
        if (limit > 50) {
            limit = 50;
        }

        long userId = StpUtil.getLoginIdAsLong();
        KeysetPageResponse<MemoryCardDeckDTO> result = deckService.getDeckList(
            postId, creatorId, state, sortBy, sortOrder, lastScore, lastId, limit, userId);

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
     * 审核拒绝卡片组 (设置为屏蔽状态)
     */
    @PostMapping("/decks/{deckId}/reject")
    public ApiResponse<Void> rejectDeck(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.rejectDeck(deckId, userId);
        return ApiResponse.success();
    }

    /**
     * 屏蔽卡片组
     */
    @PostMapping("/decks/{deckId}/block")
    public ApiResponse<Void> blockDeck(@PathVariable Long deckId) {
        long userId = StpUtil.getLoginIdAsLong();
        deckService.blockDeck(deckId, userId);
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

}