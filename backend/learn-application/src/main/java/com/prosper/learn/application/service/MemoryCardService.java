package com.prosper.learn.application.service;

import com.prosper.learn.application.assembler.CardAssembler;
import com.prosper.learn.application.dto.request.CreateCardRequest;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.request.UpdateCardRequest;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDomainService;
import com.prosper.learn.user.profile.UserDataService;
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

    // 数据服务
    private final UserDataService userDataService;

    // DTO 组装器
    private final CardAssembler cardAssembler;

    // ========== 业务方法(Query) ==========

    /**
     * 根据卡片组获取卡片列表（包含SRS状态）
     * 在read页面点击卡片组tab时调用，显示该卡片组下的所有卡片
     */
    public List<CardWithSrsDTO> getCardsByDeck(Long deckId, Long userId) {
        List<MemoryCardDO> cards = domainService.getCardsByDeck(deckId);
        return cardAssembler.toCardViewWithSrs(cards, userId);
    }

    /**
     * 根据节点ID获取用户学习的卡片列表
     */
    public List<CardWithSrsDTO> getCardsByNode(Long nodeId, Long userId) {
        MemoryCardDomainService.CardQueryResult result = domainService.getCardsByNodeWithSrs(nodeId, userId);
        return cardAssembler.toCardViewWithSrs(result.getCards(), result.getSrsStateMap(), userId);
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
    public CardWithSrsDTO createCard(Long userId, CreateCardRequest request) {
        checkNotNull(request);

        // 跨域验证
        userDataService.validateAndGet(userId);

        // 调用 DomainService 执行核心业务逻辑
        MemoryCardDO card = domainService.createCard(
            userId,
            request.getDeckId(),
            request.getFront(),
            request.getBack()
        );

        // DTO 转换
        return cardAssembler.toCardWithSrs(card, userId);
    }

    /**
     * 批量创建卡片（用于创建卡片组时同时创建卡片）
     */
    @Transactional
    public List<CardWithSrsDTO> batchCreateCards(Long userId, Long deckId, List<CreateDeckRequest.CardInfo> cardInfos) {
        checkNotNull(cardInfos);
        if (cardInfos.isEmpty()) {
            return new ArrayList<>();
        }

        // 跨域验证
        userDataService.validateAndGet(userId);

        // 转换为 CardContent
        List<MemoryCardDomainService.CardContent> cardContents = cardInfos.stream()
            .map(info -> new MemoryCardDomainService.CardContent(info.getFront(), info.getBack()))
            .collect(Collectors.toList());

        // 调用 DomainService
        List<MemoryCardDO> cards = domainService.batchCreateCards(userId, deckId, cardContents);

        // DTO 转换
        return cards.stream()
            .map(card -> cardAssembler.toCardWithSrs(card, userId))
            .collect(Collectors.toList());
    }

    /**
     * 更新卡片
     */
    @Transactional
    public CardWithSrsDTO updateCard(Long userId, Long cardId, UpdateCardRequest request) {
        checkNotNull(request);

        // 跨域验证
        userDataService.validateExists(userId);

        // 调用 DomainService
        MemoryCardDO card = domainService.updateCard(
            userId,
            cardId,
            request.getFront(),
            request.getBack()
        );

        // DTO 转换
        return cardAssembler.toCardWithSrs(card, userId);
    }

    /**
     * 删除卡片
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        // 跨域验证
        userDataService.validateExists(userId);

        // 调用 DomainService
        domainService.deleteCard(userId, cardId);
    }

    /**
     * 批量删除指定卡片组中的所有卡片
     */
    @Transactional
    public void deleteCardsByDeck(Long userId, Long deckId) {
        // 跨域验证
        userDataService.validateExists(userId);

        // 调用 DomainService
        domainService.deleteCardsByDeck(userId, deckId);
    }
}