package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.MemoryCardService;
import com.prosper.learn.dto.request.CreateCardRequest;
import com.prosper.learn.dto.request.UpdateCardRequest;
import com.prosper.learn.dto.response.MemoryCardViewDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 记忆卡片控制器
 */
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Slf4j
public class MemoryCardController {

    private final MemoryCardService cardService;

    /**
     * 创建卡片
     */
    @PostMapping("/cards")
    public ApiResponse<MemoryCardViewDTO> createCard(@Valid @RequestBody CreateCardRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        MemoryCardViewDTO result = cardService.createCard(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 更新卡片
     */
    @PutMapping("/cards/{cardId}")
    public ApiResponse<MemoryCardViewDTO> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {
        
        long userId = StpUtil.getLoginIdAsLong();
        request.setId(cardId);
        
        MemoryCardViewDTO result = cardService.updateCard(userId, request);
        return ApiResponse.success(result);
    }

    /**
     * 获取用户在指定节点下学习的所有卡片
     */
    @GetMapping("/cards/node/{nodeId}")
    public ApiResponse<java.util.List<MemoryCardViewDTO>> getUserCardsByNode(@PathVariable Long nodeId) {
        long userId = StpUtil.getLoginIdAsLong();
        java.util.List<MemoryCardViewDTO> result = cardService.getCardsByNode(nodeId, userId);
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片内容差异
     */
    @GetMapping("/cards/{cardId}/diff")
    public ApiResponse<Object> getCardDiff(@PathVariable Long cardId) {
        long userId = StpUtil.getLoginIdAsLong();
        Object result = cardService.getCardContentDiff(userId, cardId);
        return ApiResponse.success(result);
    }

    /**
     * 删除卡片
     */
    @DeleteMapping("/cards/{cardId}")
    public ApiResponse<Void> deleteCard(@PathVariable Long cardId) {
        long userId = StpUtil.getLoginIdAsLong();
        cardService.deleteCard(userId, cardId);
        return ApiResponse.success();
    }

}