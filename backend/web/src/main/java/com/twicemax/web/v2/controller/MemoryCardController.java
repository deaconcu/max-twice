package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateCardRequest;
import com.twicemax.application.dto.request.UpdateCardRequest;
import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import com.twicemax.application.service.MemoryCardService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 记忆卡片控制器
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
@Validated
public class MemoryCardController {

    private final MemoryCardService cardService;

    @PostMapping("/cards")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> createCard(
            @Valid @RequestBody CreateCardRequest request,
            @CurrentUser UserDO currentUser) {
        cardService.createCard(currentUser, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cards/{cardId}")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateCard(
            @PathVariable @NotNull(message = "卡片ID不能为空") @Positive(message = "卡片ID必须大于0") Long cardId,
            @Valid @RequestBody UpdateCardRequest request,
            @CurrentUser UserDO currentUser) {
        cardService.updateCard(currentUser, cardId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/node/{nodeId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<CardWithSrsDTO> getUserCardsByNode(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @CurrentUser UserDO currentUser) {
        return cardService.getCardsByNode(nodeId, currentUser);
    }

    @GetMapping("/cards/{cardId}/diff")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getCardDiff(
            @PathVariable @NotNull(message = "卡片ID不能为空") @Positive(message = "卡片ID必须大于0") Long cardId,
            @CurrentUser UserDO currentUser) {
        return cardService.getCardContentDiff(currentUser.getId(), cardId);
    }

    @DeleteMapping("/cards/{cardId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> deleteCard(
            @PathVariable @NotNull(message = "卡片ID不能为空") @Positive(message = "卡片ID必须大于0") Long cardId,
            @CurrentUser UserDO currentUser) {
        cardService.deleteCard(currentUser.getId(), cardId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cards/study")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> removeCardsFromStudy(
            @RequestBody @NotNull(message = "卡片ID列表不能为空")
            @Size(min = 1, max = 100, message = "卡片ID列表长度必须在1-100之间")
            List<@NotNull @Positive Long> cardIds,
            @CurrentUser UserDO currentUser) {
        cardService.removeCardsFromStudy(currentUser.getId(), cardIds);
        return ResponseEntity.noContent().build();
    }
}
