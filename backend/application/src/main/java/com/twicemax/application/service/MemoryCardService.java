package com.twicemax.application.service;

import com.twicemax.application.assembler.CardAssembler;
import com.twicemax.application.dto.request.CreateCardRequest;
import com.twicemax.application.dto.request.CreateDeckRequest;
import com.twicemax.application.dto.request.UpdateCardRequest;
import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import com.twicemax.memory.card.MemoryCardDO;
import com.twicemax.memory.card.MemoryCardDomainService;
import com.twicemax.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;

/**
 * 记忆卡片应用服务
 * 负责跨域协调、DTO转换、用户验证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardService {

    // 领域服务
    private final MemoryCardDomainService domainService;

    // DTO 组装器
    private final CardAssembler cardAssembler;

    // ========== 业务方法(Query) ==========

    /**
     * 根据卡片组获取卡片列表（包含SRS状态）
     * 在read页面点击卡片组tab时调用，显示该卡片组下的所有卡片
     */
    public List<CardWithSrsDTO> getCardsByDeck(Long deckId, UserDO user) {
        List<MemoryCardDO> cards = domainService.getCardsByDeck(deckId);
        return cardAssembler.toCardViewWithSrs(cards, user);
    }

    /**
     * 根据节点ID获取用户学习的卡片列表
     */
    public List<CardWithSrsDTO> getCardsByNode(Long nodeId, UserDO user) {
        Long userId = user != null ? user.getId() : null;
        MemoryCardDomainService.CardQueryResult result = domainService.getCardsByNodeWithSrs(nodeId, userId);
        return cardAssembler.toCardViewWithSrs(result.getCards(), result.getSrsStateMap(), user);
    }

    /**
     * 获取卡片内容差异信息
     */
    public Map<String, Object> getCardContentDiff(Long userId, Long cardId) {
        checkNotNull(cardId);
        checkNotNull(userId);
        return domainService.getCardContentDiff(userId, cardId);
    }


    // ========== 业务方法(Command) ==========

    /**
     * 创建卡片
     */
    @Transactional
    public CardWithSrsDTO createCard(UserDO user, CreateCardRequest request) {
        checkNotNull(request);
        checkNotNull(user);

        // 调用 DomainService 执行核心业务逻辑
        MemoryCardDO card = domainService.createCard(
            user.getId(),
            request.getDeckId(),
            request.getFront(),
            request.getBack()
        );

        // DTO 转换
        return cardAssembler.toCardWithSrs(card, user);
    }

    /**
     * 批量创建卡片（用于创建卡片组时同时创建卡片）
     */
    @Transactional
    public List<CardWithSrsDTO> batchCreateCards(UserDO user, Long deckId, List<CreateDeckRequest.CardInfo> cardInfos) {
        checkNotNull(cardInfos);
        checkNotNull(user);
        if (cardInfos.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为 CardContent
        List<MemoryCardDomainService.CardContent> cardContents = cardInfos.stream()
            .map(info -> new MemoryCardDomainService.CardContent(info.getFront(), info.getBack()))
            .collect(Collectors.toList());

        // 调用 DomainService
        List<MemoryCardDO> cards = domainService.batchCreateCards(user.getId(), deckId, cardContents);

        // DTO 转换
        return cards.stream()
            .map(card -> cardAssembler.toCardWithSrs(card, user))
            .collect(Collectors.toList());
    }

    /**
     * 更新卡片
     */
    @Transactional
    public CardWithSrsDTO updateCard(UserDO user, Long cardId, UpdateCardRequest request) {
        checkNotNull(request);
        checkNotNull(user);

        // 调用 DomainService
        MemoryCardDO card = domainService.updateCard(
            user.getId(),
            cardId,
            request.getFront(),
            request.getBack()
        );

        // DTO 转换
        return cardAssembler.toCardWithSrs(card, user);
    }

    /**
     * 删除卡片
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        // 调用 DomainService
        domainService.deleteCard(userId, cardId);
    }

    /**
     * 批量删除指定卡片组中的所有卡片
     */
    @Transactional
    public void deleteCardsByDeck(Long userId, Long deckId) {
        // 调用 DomainService
        domainService.deleteCardsByDeck(userId, deckId);
    }

    /**
     * 全局移除卡片学习记录（从所有课程的复习计划中移除）
     */
    @Transactional
    public void removeCardsFromStudy(Long userId, List<Long> cardIds) {
        checkNotNull(cardIds);
        if (cardIds.isEmpty()) {
            return;
        }

        // 调用 DomainService
        domainService.removeCardsFromStudy(userId, cardIds);
    }
}
