package com.prosper.learn.memory.card;

import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 记忆卡片领域服务
 * 只依赖本领域（memory）模块，处理核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDomainService {

    private final MemoryCardDataService cardDataService;
    private final MemoryCardVersionDataService cardVersionDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final UserCardSrsDataService userCardSrsDataService;

    // ========== Query 方法 ==========

    /**
     * 根据卡片组ID获取卡片列表
     */
    public List<MemoryCardDO> getCardsByDeck(Long deckId) {
        deckDataService.validateExists(deckId);
        return cardDataService.getByDeckId(deckId);
    }

    /**
     * 根据节点ID获取用户学习的卡片列表（包含SRS状态）
     */
    public CardQueryResult getCardsByNodeWithSrs(Long nodeId, Long userId) {
        List<UserCardSrsDO> srsList = userCardSrsDataService.getByUserAndNodeId(userId, nodeId);

        if (srsList.isEmpty()) {
            return new CardQueryResult(Collections.emptyList(), Collections.emptyMap());
        }

        List<Long> cardIds = srsList.stream()
            .map(UserCardSrsDO::getCardId)
            .collect(Collectors.toList());

        List<MemoryCardDO> cards = cardDataService.getByIds(cardIds);

        Map<Long, UserCardSrsDO> srsStateMap = srsList.stream()
            .collect(Collectors.toMap(UserCardSrsDO::getCardId, srs -> srs));

        return new CardQueryResult(cards, srsStateMap);
    }

    /**
     * 获取卡片内容差异信息
     */
    public Map<String, Object> getCardContentDiff(Long userId, Long cardId) {
        MemoryCardDO card = cardDataService.validateAndGet(cardId);
        if (card.getState() != ContentState.PUBLISHED.value()) {
            throw StatusCode.MEMORY_CARD_NOT_AVAILABLE.exception();
        }

        UserCardSrsDO srsState = userCardSrsDataService.getByUserAndCard(userId, cardId);
        if (srsState == null || srsState.getCardVersionId() == null) {
            throw StatusCode.INVALID_PARAMETER.exception("用户未学习此卡片，无法查看差异");
        }

        MemoryCardVersionDO oldVersion = cardVersionDataService.getById(srsState.getCardVersionId());
        if (oldVersion == null) {
            throw StatusCode.SYSTEM_ERROR.exception("用户学习版本不存在");
        }

        MemoryCardVersionDO newVersion = cardVersionDataService.getById(card.getCurrentVersionId());
        if (newVersion == null) {
            throw StatusCode.SYSTEM_ERROR.exception("卡片最新版本不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cardId", cardId);

        Map<String, String> oldVersionMap = new HashMap<>();
        oldVersionMap.put("front", oldVersion.getFront());
        oldVersionMap.put("back", oldVersion.getBack());
        result.put("oldVersion", oldVersionMap);

        Map<String, String> newVersionMap = new HashMap<>();
        newVersionMap.put("front", newVersion.getFront());
        newVersionMap.put("back", newVersion.getBack());
        result.put("newVersion", newVersionMap);

        return result;
    }

    // ========== Command 方法 ==========

    /**
     * 创建卡片（核心业务逻辑）
     */
    @Transactional
    public MemoryCardDO createCard(Long userId, Long deckId, String front, String back) {
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        if (!deck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限在此卡片组中创建卡片");
        }

        String contentHash = calculateContentHash(front, back);

        MemoryCardDO card = new MemoryCardDO();
        card.setDeckId(deckId);
        card.setCreatorId(userId);
        card.setCurrentVersionId(0L);
        card.setState(ContentState.PUBLISHED.value());

        int cardResult = cardDataService.insert(card);
        if (cardResult <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("创建卡片失败");
        }

        MemoryCardVersionDO version = new MemoryCardVersionDO();
        version.setCardId(card.getId());
        version.setVersion(1);
        version.setCreatorId(userId);
        version.setFront(front);
        version.setBack(back);
        version.setContentHash(contentHash);
        version.setIsActive(true);

        int versionResult = cardVersionDataService.insert(version);
        if (versionResult <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("创建卡片版本失败");
        }

        card.setCurrentVersionId(version.getId());
        cardDataService.update(card);

        deckDataService.incrementCardCountAndSetStateAndVersion(deckId, ContentState.SUBMITTED.value());

        log.info("Created card: {} in deck: {} by user: {}", card.getId(), deckId, userId);

        return cardDataService.getById(card.getId());
    }

    /**
     * 批量创建卡片
     */
    @Transactional
    public List<MemoryCardDO> batchCreateCards(Long userId, Long deckId, List<CardContent> cardContents) {
        if (cardContents.isEmpty()) {
            return Collections.emptyList();
        }

        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        if (!deck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限在此卡片组中创建卡片");
        }

        LocalDateTime now = LocalDateTime.now();

        List<MemoryCardDO> cardsToInsert = new ArrayList<>();
        for (CardContent cardContent : cardContents) {
            MemoryCardDO card = new MemoryCardDO();
            card.setDeckId(deckId);
            card.setCreatorId(userId);
            card.setCurrentVersionId(0L);
            card.setState(ContentState.PUBLISHED.value());
            card.setCreatedAt(now);
            card.setUpdatedAt(now);
            cardsToInsert.add(card);
        }

        int cardResult = cardDataService.batchInsert(cardsToInsert);
        if (cardResult <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("批量创建卡片失败");
        }

        List<MemoryCardVersionDO> versionsToInsert = new ArrayList<>();
        for (int i = 0; i < cardContents.size(); i++) {
            CardContent cardContent = cardContents.get(i);
            MemoryCardDO card = cardsToInsert.get(i);

            String contentHash = calculateContentHash(cardContent.getFront(), cardContent.getBack());

            MemoryCardVersionDO version = new MemoryCardVersionDO();
            version.setCardId(card.getId());
            version.setVersion(1);
            version.setCreatorId(userId);
            version.setFront(cardContent.getFront());
            version.setBack(cardContent.getBack());
            version.setContentHash(contentHash);
            version.setIsActive(true);
            version.setCreatedAt(now);
            versionsToInsert.add(version);
        }

        int versionResult = cardVersionDataService.batchInsert(versionsToInsert);
        if (versionResult <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("批量创建卡片版本失败");
        }

        for (int i = 0; i < cardsToInsert.size(); i++) {
            MemoryCardDO card = cardsToInsert.get(i);
            MemoryCardVersionDO version = versionsToInsert.get(i);
            card.setCurrentVersionId(version.getId());
        }
        cardDataService.batchUpdateCurrentVersionId(cardsToInsert);

        log.info("Batch created {} cards for deck {} by user {}", cardContents.size(), deckId, userId);

        return cardsToInsert;
    }

    /**
     * 更新卡片
     */
    @Transactional
    public MemoryCardDO updateCard(Long userId, Long cardId, String front, String back) {
        MemoryCardDO existingCard = cardDataService.validateAndGet(cardId);

        if (!existingCard.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限修改此卡片");
        }

        MemoryCardVersionDO currentVersion = cardVersionDataService.getById(existingCard.getCurrentVersionId());
        if (currentVersion == null) {
            throw StatusCode.SYSTEM_ERROR.exception("卡片版本不存在");
        }

        String contentHash = calculateContentHash(front, back);
        if (contentHash.equals(currentVersion.getContentHash())) {
            return existingCard;
        }

        MemoryCardVersionDO newVersion = new MemoryCardVersionDO();
        newVersion.setCardId(cardId);
        newVersion.setVersion(currentVersion.getVersion() + 1);
        newVersion.setCreatorId(userId);
        newVersion.setFront(front);
        newVersion.setBack(back);
        newVersion.setContentHash(contentHash);
        newVersion.setIsActive(true);

        int versionResult = cardVersionDataService.insert(newVersion);
        if (versionResult <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("创建新版本失败");
        }

        cardVersionDataService.updateActiveStatus(currentVersion.getId(), false);

        MemoryCardDO card = cardDataService.getById(cardId);
        card.setCurrentVersionId(newVersion.getId());
        card.setUpdatedAt(LocalDateTime.now());
        cardDataService.update(card);

        log.info("Updated card: {} to version: {} by user: {}", cardId, newVersion.getVersion(), userId);

        deckDataService.updateStateAndIncrementVersion(existingCard.getDeckId(), ContentState.SUBMITTED.value());

        return cardDataService.getById(cardId);
    }

    /**
     * 删除卡片
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        MemoryCardDO card = cardDataService.validateAndGet(cardId);

        if (!card.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限删除此卡片");
        }

        MemoryCardDeckDO deck = deckDataService.getById(card.getDeckId());
        if (deck == null) {
            throw StatusCode.SYSTEM_ERROR.exception("卡片组不存在");
        }

        card.setDeletedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());
        int result = cardDataService.softDelete(card);

        if (result == 0) {
            throw StatusCode.MEMORY_CARD_NOT_FOUND.exception("卡片已被删除或不存在");
        }

        deckDataService.decrementCardCountAndIncrementVersion(card.getDeckId());

        log.info("Deleted card: {} from deck: {} by user: {}", cardId, card.getDeckId(), userId);
    }

    /**
     * 批量删除指定卡片组中的所有卡片
     */
    @Transactional
    public void deleteCardsByDeck(Long userId, Long deckId) {
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        if (!deck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限删除此卡片组中的卡片");
        }

        List<MemoryCardDO> cards = cardDataService.getByDeckId(deckId);
        if (cards.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (MemoryCardDO card : cards) {
            card.setState(ContentState.BANNED.value());
            card.setUpdatedAt(now);
        }

        cardDataService.batchUpdate(cards);

        deck.setCardCount(0);
        deck.setVersion(deck.getVersion() + 1);
        deck.setUpdatedAt(now);
        deckDataService.update(deck);

        log.info("Batch deleted {} cards from deck: {} by user: {}", cards.size(), deckId, userId);
    }

    /**
     * 全局移除卡片学习记录（从复习计划中移除）
     * 直接删除 user_card_srs 记录（现在一张卡片只属于一个课程）
     */
    @Transactional
    public void removeCardsFromStudy(Long userId, List<Long> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) {
            return;
        }

        // 删除 SRS 学习状态
        int srsRecordsDeleted = userCardSrsDataService.batchDeleteByUserAndCards(userId, cardIds);

        log.info("Removed {} cards from study for user: {}, deleted {} SRS records",
            cardIds.size(), userId, srsRecordsDeleted);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 计算内容哈希
     */
    private String calculateContentHash(String front, String back) {
        String content = (front != null ? front : "") + "|" + (back != null ? back : "");
        return Utils.hashSHA(content);
    }

    // ========== 内部类 ==========

    /**
     * 卡片查询结果（包含SRS状态）
     */
    public static class CardQueryResult {
        private final List<MemoryCardDO> cards;
        private final Map<Long, UserCardSrsDO> srsStateMap;

        public CardQueryResult(List<MemoryCardDO> cards, Map<Long, UserCardSrsDO> srsStateMap) {
            this.cards = cards;
            this.srsStateMap = srsStateMap;
        }

        public List<MemoryCardDO> getCards() {
            return cards;
        }

        public Map<Long, UserCardSrsDO> getSrsStateMap() {
            return srsStateMap;
        }
    }

    /**
     * 卡片内容（用于批量创建）
     */
    public static class CardContent {
        private final String front;
        private final String back;

        public CardContent(String front, String back) {
            this.front = front;
            this.back = back;
        }

        public String getFront() {
            return front;
        }

        public String getBack() {
            return back;
        }
    }
}
