package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.AcceptDeckChangesRequest;
import com.twicemax.application.dto.request.CreateDeckRequest;
import com.twicemax.application.dto.request.UpdateDeckRequest;
import com.twicemax.application.dto.response.deck.DeckAndCardsDTO;
import com.twicemax.application.dto.response.deck.DeckFullDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.MemoryCardDeckService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
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

import java.util.concurrent.TimeUnit;

/**
 * 记忆卡片组控制器
 */
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
@Validated
public class MemoryCardDeckController {

    private final MemoryCardDeckService deckService;

    @GetMapping("/posts/{postId}/decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getPostPublicDecks(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return deckService.getPostPublicDecks(postId, sortBy, cursor, 20, currentUser != null ? currentUser.getId() : null);
    }

    @GetMapping("/posts/{postId}/creator-deck")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getPostCreatorDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return deckService.getPostCreatorDeck(postId, cursor, 20, currentUser.getId());
    }

    @GetMapping("/posts/{postId}/my-deck")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getMyPostDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return deckService.getMyPostDeck(postId, currentUser.getId(), sortBy, cursor, 20);
    }

    @GetMapping("/users/me/memory-decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getCurrentUserAllDecks(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer state,
            @CurrentUser UserDO currentUser) {
        Byte stateValue = null;
        if (state != null) {
            if (state.byteValue() == Enums.ContentState.BANNED.value()) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
            stateValue = state.byteValue();
        }
        return deckService.getUserDecks(currentUser.getId(), currentUser.getId(), cursor, 20, stateValue);
    }

    @GetMapping("/users/{userId}/memory-decks")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getUserDecks(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return deckService.getUserDecks(userId, currentUser.getId(), cursor, 20, Enums.ContentState.PUBLISHED.value());
    }

    @GetMapping("/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public DeckAndCardsDTO getDeckDetail(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @CurrentUser UserDO currentUser) {
        return deckService.getDeckDetail(deckId, currentUser);
    }

    @GetMapping("/decks/node/{nodeId}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<DeckFullDTO> getDecksByNode(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive(message = "限制数量必须大于0") Integer limit,
            @CurrentUser UserDO currentUser) {
        return deckService.getDecksByNode(nodeId, cursor, limit, currentUser.getId());
    }

    @PostMapping("/decks")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> createDeck(
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {
        deckService.createDeck(currentUser, request);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/decks/{deckId}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @Valid @RequestBody UpdateDeckRequest request,
            @CurrentUser UserDO currentUser) {
        deckService.updateDeck(currentUser.getId(), deckId, request.getDescription());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decks/{deckId}/diff")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Object getDeckDiff(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestParam(required = false) @Positive(message = "版本号必须大于0") Integer userCurrentVersion,
            @CurrentUser UserDO currentUser) {
        return deckService.getDeckDiff(deckId, userCurrentVersion, currentUser.getId());
    }

    @PostMapping("/decks/{deckId}/accept-changes")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> acceptDeckChanges(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @RequestBody AcceptDeckChangesRequest request,
            @CurrentUser UserDO currentUser) {
        deckService.acceptDeckChanges(deckId, request.getCardIds(), request.getCourseId(),
                Boolean.TRUE.equals(request.getRemoveOtherDeckCards()), currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/decks/{deckId}/cards")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> replaceAllCards(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long deckId,
            @Valid @RequestBody CreateDeckRequest request,
            @CurrentUser UserDO currentUser) {
        deckService.replaceAllCards(currentUser, deckId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/decks/{postId}/ai-generate")
    @SaCheckLogin
    @RateLimit(capacity = 10, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> createAIDeck(
            @PathVariable @NotNull(message = "帖子ID不能为空") @Positive(message = "帖子ID必须大于0") Long postId,
            @CurrentUser UserDO currentUser) {
        deckService.createAIDeck(currentUser.getId(), postId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/decks/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> deleteDeck(
            @PathVariable @NotNull(message = "卡片组ID不能为空") @Positive(message = "卡片组ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        deckService.deleteDeck(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nodes/{nodeId}/move-to-course")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> moveNodeToCourse(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        deckService.moveNodeToCourse(currentUser.getId(), nodeId, courseId);
        return ResponseEntity.noContent().build();
    }
}
