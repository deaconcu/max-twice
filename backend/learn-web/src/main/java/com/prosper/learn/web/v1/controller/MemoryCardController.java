package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreateCardRequest;
import com.prosper.learn.application.dto.request.UpdateCardRequest;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.service.MemoryCardService;
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
import java.util.List;

/**
 * 记忆卡片控制器
 */
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Slf4j
@Validated
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class MemoryCardController {

    private final MemoryCardService cardService;

    /**
     * 创建卡片
     */
    @PostMapping("/cards")
    @SaCheckLogin
    public ApiResponse<Void> createCard(
            @Valid @RequestBody CreateCardRequest request,
            @CurrentUser UserDO currentUser) {
        cardService.createCard(currentUser.getId(), request);
        return ApiResponse.success("创建成功", null);
    }

    /**
     * 更新卡片
     */
    @PutMapping("/cards/{cardId}")
    @SaCheckLogin
    public ApiResponse<Void> updateCard(
            @PathVariable @NotNull(message = "卡片ID不能为空")
            @Positive(message = "卡片ID必须大于0")
            Long cardId,
            @Valid @RequestBody UpdateCardRequest request,
            @CurrentUser UserDO currentUser) {

        request.setId(cardId);

        cardService.updateCard(currentUser.getId(), request);
        return ApiResponse.success("更新成功", null);
    }

    /**
     * 获取用户在指定节点下学习的所有卡片
     */
    @GetMapping("/cards/node/{nodeId}")
    @SaCheckLogin
    public ApiResponse<List<CardWithSrsDTO>> getUserCardsByNode(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @CurrentUser UserDO currentUser) {
        List<CardWithSrsDTO> result = cardService.getCardsByNode(nodeId, currentUser.getId());
        return ApiResponse.success(result);
    }

    /**
     * 获取卡片内容差异
     */
    @GetMapping("/cards/{cardId}/diff")
    @SaCheckLogin
    public ApiResponse<Object> getCardDiff(
            @PathVariable @NotNull(message = "卡片ID不能为空")
            @Positive(message = "卡片ID必须大于0")
            Long cardId,
            @CurrentUser UserDO currentUser) {
        Object result = cardService.getCardContentDiff(currentUser.getId(), cardId);
        return ApiResponse.success(result);
    }

    /**
     * 删除卡片
     */
    @DeleteMapping("/cards/{cardId}")
    @SaCheckLogin
    public ApiResponse<Void> deleteCard(
            @PathVariable @NotNull(message = "卡片ID不能为空")
            @Positive(message = "卡片ID必须大于0")
            Long cardId,
            @CurrentUser UserDO currentUser) {
        cardService.deleteCard(currentUser.getId(), cardId);
        return ApiResponse.success("删除成功", null);
    }

}