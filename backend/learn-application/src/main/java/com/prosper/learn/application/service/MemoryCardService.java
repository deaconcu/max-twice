package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.UserCardSrsConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.request.CreateCardRequest;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.request.UpdateCardRequest;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDomainService;
import com.prosper.learn.memory.card.MemoryCardVersionDO;
import com.prosper.learn.memory.card.MemoryCardVersionDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.user.profile.UserDO;
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
    private final MemoryCardVersionDataService cardVersionDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final UserDataService userDataService;
    private final UserCardSrsDataService userCardSrsDataService;

    // 转换器
    private final UserConverter userConverter;
    private final UserCardSrsConverter srsStateConverter;
    private final UserService userService;
    private final MemoryCardDeckConverter deckConverter;

    // ========== toDTO ==========

    /**
     * 转换为卡片（含SRS状态）
     */
    public CardWithSrsDTO toCardWithSrs(MemoryCardDO cardDO, Long userId) {
        if (cardDO == null) return null;

        CardWithSrsDTO dto = new CardWithSrsDTO();
        dto.setId(cardDO.getId());

        // 填充创建者信息
        dto.setCreator(userService.toBriefDTO(userDataService.getById(cardDO.getCreatorId())));

        MemoryCardDeckDO deck = deckDataService.validateAndGet(cardDO.getDeckId());
        dto.setDeck(deckConverter.toSummaryDTO(deck));

        // 获取SRS状态并检测更新
        UserCardSrsDO srsState = null;
        if (userId != null) {
            srsState = userCardSrsDataService.getByUserAndCard(userId, cardDO.getId());
            if (srsState != null) {
                dto.setSrsState(srsStateConverter.toDTO(srsState));

                // 检测deck是否有更新：比较用户学习时的deck版本和当前deck版本
                boolean hasDeckUpdate = srsState.getDeckVersion() != null &&
                    !srsState.getDeckVersion().equals(deck.getVersion());
                dto.setHasDeckUpdate(hasDeckUpdate);

                // 检测卡片内容是否有更新：比较用户学习时的卡片版本和当前最新版本
                boolean hasCardUpdate = srsState.getCardVersionId() != null &&
                    !srsState.getCardVersionId().equals(cardDO.getCurrentVersionId());
                dto.setHasCardUpdate(hasCardUpdate);
            }
        }

        // 获取卡片内容版本
        Long versionIdToUse;
        if (userId != null && srsState != null && srsState.getCardVersionId() != null) {
            // 如果传入了userId且用户有学习记录，使用用户学习时的版本（复习/学习卡片场景）
            versionIdToUse = srsState.getCardVersionId();
        } else {
            // 如果没有传入userId或用户没有学习记录，使用最新版本（所有卡片场景）
            versionIdToUse = cardDO.getCurrentVersionId();
        }

        if (versionIdToUse != null) {
            MemoryCardVersionDO version = cardVersionDataService.getById(versionIdToUse);
            if (version != null) {
                dto.setFront(version.getFront());
                dto.setBack(version.getBack());
            }
        }

        return dto;
    }

// --注释掉检查 START (2025/12/10 11:15):
//    public CardWithSrsDTO toCardViewWithSrs(MemoryCardDO cardDO) {
//        return toCardWithSrs(cardDO, null);
//    }
// --注释掉检查 STOP (2025/12/10 11:15)

    public List<CardWithSrsDTO> toCardViewWithSrs(List<MemoryCardDO> cardDOList,
                                           Map<Long, UserCardSrsDO> srsStateMap,
                                           Long userId) {
        if (cardDOList == null || cardDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取创建者信息
        Set<Long> creatorIds = cardDOList.stream()
                .map(MemoryCardDO::getCreatorId)
                .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = userDataService.getMapByIds(creatorIds);

        // 批量获取卡片版本内容 - 需要同时获取最新版本和用户学习时的版本
        Set<Long> allVersionIds = new HashSet<>();

        // 添加所有卡片的最新版本ID
        cardDOList.stream()
                .map(MemoryCardDO::getCurrentVersionId)
                .filter(Objects::nonNull)
                .forEach(allVersionIds::add);

        // 添加用户学习时的版本ID
        srsStateMap.values().stream()
                .map(UserCardSrsDO::getCardVersionId)
                .filter(Objects::nonNull)
                .forEach(allVersionIds::add);

        Map<Long, MemoryCardVersionDO> versionMap = cardVersionDataService.getMapByIds(allVersionIds);

        Set<Long> deckIds = cardDOList.stream()
                .map(MemoryCardDO::getDeckId)
                .collect(Collectors.toSet());
        Map<Long, MemoryCardDeckDO> deckMap = deckDataService.getMapByIds(deckIds);

        return cardDOList.stream()
                .map(card -> {
                    CardWithSrsDTO dto = new CardWithSrsDTO();
                    dto.setId(card.getId());

                    // 设置创建者信息
                    UserDO creator = userMap.get(card.getCreatorId());
                    if (creator != null) {
                        dto.setCreator(userConverter.toBriefDTO(creator));
                    }

                    // 获取SRS状态并检测更新
                    UserCardSrsDO srsState = srsStateMap.get(card.getId());
                    if (srsState != null) {
                        dto.setSrsState(srsStateConverter.toDTO(srsState));

                        // 检测deck是否有更新
                        MemoryCardDeckDO deck = deckMap.get(card.getDeckId());
                        if (deck != null && srsState.getDeckVersion() != null) {
                            boolean hasDeckUpdate = !srsState.getDeckVersion().equals(deck.getVersion());
                            dto.setHasDeckUpdate(hasDeckUpdate);
                        }

                        // 检测卡片内容是否有更新
                        if (srsState.getCardVersionId() != null) {
                            boolean hasCardUpdate = !srsState.getCardVersionId().equals(card.getCurrentVersionId());
                            dto.setHasCardUpdate(hasCardUpdate);
                        }
                    }

                    // 设置卡片内容 - 根据场景选择版本
                    Long versionIdToUse;
                    if (userId != null && srsState != null && srsState.getCardVersionId() != null) {
                        // 如果传入了userId且用户有学习记录，使用用户学习时的版本（复习/学习卡片场景）
                        versionIdToUse = srsState.getCardVersionId();
                    } else {
                        // 如果没有传入userId或用户没有学习记录，使用最新版本（所有卡片场景）
                        versionIdToUse = card.getCurrentVersionId();
                    }

                    if (versionIdToUse != null) {
                        MemoryCardVersionDO version = versionMap.get(versionIdToUse);
                        if (version != null) {
                            dto.setFront(version.getFront());
                            dto.setBack(version.getBack());
                        }
                    }

                    if (deckMap.containsKey(card.getDeckId())) {
                        dto.setDeck(deckConverter.toSummaryDTO(deckMap.get(card.getDeckId())));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<CardWithSrsDTO> toCardViewWithSrs(List<MemoryCardDO> cardDOList, Long userId) {
        if (cardDOList == null || cardDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取SRS状态
        Map<Long, UserCardSrsDO> srsStateMap;
        if (userId != null) {
            Set<Long> cardIds = cardDOList.stream()
                .map(MemoryCardDO::getId)
                .collect(Collectors.toSet());
            List<UserCardSrsDO> srsStates = userCardSrsDataService.getByUserAndCards(userId, cardIds);
            srsStateMap = srsStates.stream()
                .collect(Collectors.toMap(UserCardSrsDO::getCardId, s -> s));
        } else {
            srsStateMap = new HashMap<>();
        }

        return toCardViewWithSrs(cardDOList, srsStateMap, userId);
    }

    /**
     * 单张卡片转换为包含SRS状态的DTO
     */
    public CardWithSrsDTO toCardViewWithSrs(MemoryCardDO cardDO, Long userId) {
        if (cardDO == null) {
            return null;
        }
        List<CardWithSrsDTO> result = toCardViewWithSrs(List.of(cardDO), userId);
        return result.isEmpty() ? null : result.get(0);
    }


    // ========== 业务方法(Query) ==========

    /**
     * 根据卡片组获取卡片列表（包含SRS状态）
     * 在read页面点击卡片组tab时调用，显示该卡片组下的所有卡片
     */
    public List<CardWithSrsDTO> getCardsByDeck(Long deckId, Long userId) {
        List<MemoryCardDO> cards = domainService.getCardsByDeck(deckId);
        return toCardViewWithSrs(cards, userId);
    }

    /**
     * 根据节点ID获取用户学习的卡片列表
     */
    public List<CardWithSrsDTO> getCardsByNode(Long nodeId, Long userId) {
        MemoryCardDomainService.CardQueryResult result = domainService.getCardsByNodeWithSrs(nodeId, userId);
        return toCardViewWithSrs(result.getCards(), result.getSrsStateMap(), userId);
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
        return toCardWithSrs(card, userId);
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
            .map(card -> toCardWithSrs(card, userId))
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
        return toCardWithSrs(card, userId);
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