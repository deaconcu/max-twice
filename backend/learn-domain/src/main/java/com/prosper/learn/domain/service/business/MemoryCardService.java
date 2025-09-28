package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.*;
import com.prosper.learn.domain.util.converter.*;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.dto.response.*;
import com.prosper.learn.persistence.dataobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;
import static com.prosper.learn.common.Enums.*;

/**
 * 记忆卡片业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardService {

    private final MemoryCardDataService cardDataService;
    private final MemoryCardVersionDataService cardVersionDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDataService memoryCardDataService;
    private final UserDataService userDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final MemoryCardConverter cardConverter;
    private final UserConverter userConverter;
    private final UserCardSrsConverter srsStateConverter;
    private final UserService userService;
    private final MemoryCardDeckConverter deckConverter;

    // ========== toDTO ==========

    /**
     * V1 = 基础信息 + 创建者信息 + 卡片内容 + SRS状态(可选) + 更新检测
     */
    public MemoryCardViewDTO toDTOV1(MemoryCardDO cardDO, Long userId) {
        if (cardDO == null) return null;

        MemoryCardViewDTO dto = new MemoryCardViewDTO();
        dto.setId(cardDO.getId());

        // 填充创建者信息
        UserDTO creator = userService.getUser(cardDO.getCreatorId(), DTOVersion.V2);
        dto.setCreator(creator);

        MemoryCardDeckDO deck = deckDataService.validateAndGet(cardDO.getDeckId());
        dto.setDeck(deckConverter.toDTOV2(deck));

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

    public MemoryCardViewDTO toDTOV1(MemoryCardDO cardDO) {
        return toDTOV1(cardDO, null);
    }

    public List<MemoryCardViewDTO> toDTOV1(List<MemoryCardDO> cardDOList,
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
                    MemoryCardViewDTO dto = new MemoryCardViewDTO();
                    dto.setId(card.getId());

                    // 设置创建者信息
                    UserDO creator = userMap.get(card.getCreatorId());
                    if (creator != null) {
                        dto.setCreator(userConverter.toDTO(creator));
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
                        dto.setDeck(deckConverter.toDTOV2(deckMap.get(card.getDeckId())));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<MemoryCardViewDTO> toDTOV1(List<MemoryCardDO> cardDOList, Long userId) {
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

        return toDTOV1(cardDOList, srsStateMap, userId);
    }


    // ========== 业务方法(Query) ==========

    /**
     * 根据卡片组获取卡片列表（包含SRS状态）
     * 在read页面点击卡片组tab时调用，显示该卡片组下的所有卡片
     */
    public List<MemoryCardViewDTO> getCardsByDeck(Long deckId, Long userId) {
        // 验证卡片组存在
        deckDataService.validateExists(deckId);

        // 获取卡片列表并转换为DTO（包含SRS状态）
        List<MemoryCardDO> cards = cardDataService.getByDeckId(deckId);
        return toDTOV1(cards, userId);
    }

    /**
     * 根据节点ID获取用户学习的卡片列表
     */
    public List<MemoryCardViewDTO> getCardsByNode(Long nodeId, Long userId) {
        // 获取用户在这个节点下的所有SRS学习记录
        List<UserCardSrsDO> srsList = userCardSrsDataService.getByUserAndNodeId(userId, nodeId);

        if (srsList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取卡片ID列表
        List<Long> cardIds = srsList.stream()
            .map(UserCardSrsDO::getCardId)
            .collect(Collectors.toList());

        // 批量获取卡片信息
        List<MemoryCardDO> cards = memoryCardDataService.getByIds(cardIds);

        // 创建SRS状态映射表
        Map<Long, UserCardSrsDO> srsStateMap = srsList.stream()
            .collect(Collectors.toMap(UserCardSrsDO::getCardId, srs -> srs));

        // 转换为DTO，使用用户学习时的版本
        return toDTOV1(cards, srsStateMap, userId);
    }

    /**
     * 获取卡片内容差异信息
     */
    public Map<String, Object> getCardContentDiff(Long userId, Long cardId) {
        // 验证参数
        checkNotNull(cardId);
        checkNotNull(userId);

        // 获取卡片信息
        MemoryCardDO card = cardDataService.validateAndGet(cardId);

        // 获取用户的SRS学习记录
        UserCardSrsDO srsState = userCardSrsDataService.getByUserAndCard(userId, cardId);
        if (srsState == null || srsState.getCardVersionId() == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户未学习此卡片，无法查看差异");
        }

        // 获取用户学习时的版本
        MemoryCardVersionDO oldVersion = cardVersionDataService.getById(srsState.getCardVersionId());
        if (oldVersion == null) {
            throw ErrorCode.SYSTEM_ERROR.exception("用户学习版本不存在");
        }

        // 获取当前最新版本
        MemoryCardVersionDO newVersion = cardVersionDataService.getById(card.getCurrentVersionId());
        if (newVersion == null) {
            throw ErrorCode.SYSTEM_ERROR.exception("卡片最新版本不存在");
        }

        // 构建差异信息
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


    // ========== 业务方法(Command) ==========

    /**
     * 创建卡片
     */
    @Transactional
    public MemoryCardViewDTO createCard(Long userId, CreateCardRequest request) {
        // 验证参数
        checkNotNull(request);

        // 验证并获取用户和卡片组（需要使用对象）
        UserDO user = userDataService.validateAndGet(userId);
        MemoryCardDeckDO deck = deckDataService.validateAndGet(request.getDeckId());

        // 验证权限：只有卡片组创建者可以添加卡片
        if (!deck.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限在此卡片组中创建卡片");
        }

        // 计算内容哈希
        String contentHash = calculateContentHash(request.getFront(), request.getBack());

        // 创建卡片
        MemoryCardDO card = new MemoryCardDO();
        card.setDeckId(request.getDeckId());
        card.setCreatorId(userId);
        card.setCurrentVersionId(0L); // 临时设置为0，版本插入后再更新
        card.setState(MemoryCardState.NORMAL.value()); // 正常状态
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        int cardResult = cardDataService.insert(card);
        if (cardResult <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("创建卡片失败");
        }

        // 创建卡片版本
        MemoryCardVersionDO version = new MemoryCardVersionDO();
        version.setCardId(card.getId());
        version.setVersion(1);
        version.setCreatorId(userId);
        version.setFront(request.getFront());
        version.setBack(request.getBack());
        version.setContentHash(contentHash);
        version.setIsActive(true);
        version.setCreatedAt(LocalDateTime.now());

        int versionResult = cardVersionDataService.insert(version);
        if (versionResult <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("创建卡片版本失败");
        }

        // 更新卡片的当前版本ID
        card.setCurrentVersionId(version.getId());
        cardDataService.update(card);

        // 原子操作：更新卡片组的卡片数量、状态和版本（卡片内容变化，需要重新审核）
        deckDataService.incrementCardCountAndSetStateAndVersion(request.getDeckId(), MemoryCardDeckState.PENDING.value());

        log.info("Created card: {} in deck: {} by user: {}", card.getId(), request.getDeckId(), userId);

        // 转换并返回
        return toDTOV1(card, userId);
    }

    /**
     * 批量创建卡片（用于创建卡片组时同时创建卡片）
     */
    @Transactional
    public List<MemoryCardViewDTO> batchCreateCards(Long userId, Long deckId, List<CreateDeckRequest.CardInfo> cardInfos) {
        // 验证参数
        checkNotNull(cardInfos);
        if (cardInfos.isEmpty()) {
            return new ArrayList<>();
        }

        // 验证并获取用户和卡片组
        UserDO user = userDataService.validateAndGet(userId);
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证权限：只有卡片组创建者可以添加卡片
        if (!deck.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限在此卡片组中创建卡片");
        }

        LocalDateTime now = LocalDateTime.now();
        
        // 准备批量插入的卡片
        List<MemoryCardDO> cardsToInsert = new ArrayList<>();
        for (CreateDeckRequest.CardInfo cardInfo : cardInfos) {
            MemoryCardDO card = new MemoryCardDO();
            card.setDeckId(deckId);
            card.setCreatorId(userId);
            card.setCurrentVersionId(0L); // 临时设置为0，版本插入后再更新
            card.setState(MemoryCardState.NORMAL.value());
            card.setCreatedAt(now);
            card.setUpdatedAt(now);
            cardsToInsert.add(card);
        }

        // 批量插入卡片
        int cardResult = cardDataService.batchInsert(cardsToInsert);
        if (cardResult <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("批量创建卡片失败");
        }

        // 准备批量插入的卡片版本
        List<MemoryCardVersionDO> versionsToInsert = new ArrayList<>();
        for (int i = 0; i < cardInfos.size(); i++) {
            CreateDeckRequest.CardInfo cardInfo = cardInfos.get(i);
            MemoryCardDO card = cardsToInsert.get(i);
            
            String contentHash = calculateContentHash(cardInfo.getFront(), cardInfo.getBack());
            
            MemoryCardVersionDO version = new MemoryCardVersionDO();
            version.setCardId(card.getId());
            version.setVersion(1);
            version.setCreatorId(userId);
            version.setFront(cardInfo.getFront());
            version.setBack(cardInfo.getBack());
            version.setContentHash(contentHash);
            version.setIsActive(true);
            version.setCreatedAt(now);
            versionsToInsert.add(version);
        }

        // 批量插入卡片版本
        int versionResult = cardVersionDataService.batchInsert(versionsToInsert);
        if (versionResult <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("批量创建卡片版本失败");
        }

        // 批量更新卡片的当前版本ID
        for (int i = 0; i < cardsToInsert.size(); i++) {
            MemoryCardDO card = cardsToInsert.get(i);
            MemoryCardVersionDO version = versionsToInsert.get(i);
            card.setCurrentVersionId(version.getId());
        }
        cardDataService.batchUpdateCurrentVersionId(cardsToInsert);

        log.info("Batch created {} cards for deck {} by user {}", cardInfos.size(), deckId, userId);

        // 转换为DTO返回
        return cardsToInsert.stream()
            .map(card -> toDTOV1(card, userId))
            .collect(Collectors.toList());
    }

    /**
     * 更新卡片
     */
    @Transactional
    public MemoryCardViewDTO updateCard(Long userId, UpdateCardRequest request) {
        // 验证参数
        checkNotNull(request);

        // 验证并获取卡片（需要使用对象）
        userDataService.validateExists(userId);
        MemoryCardDO existingCard = cardDataService.validateAndGet(request.getId());

        // 验证权限：只有卡片创建者可以修改
        if (!existingCard.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限修改此卡片");
        }

        // 获取当前版本
        MemoryCardVersionDO currentVersion = cardVersionDataService.getById(existingCard.getCurrentVersionId());
        if (currentVersion == null) {
            throw ErrorCode.SYSTEM_ERROR.exception("卡片版本不存在");
        }


        // 计算新内容哈希
        String contentHash = calculateContentHash(request.getFront(), request.getBack());
        // 检查内容是否有变化，没有变化直接返回
        if (contentHash.equals(currentVersion.getContentHash())) {
            return toDTOV1(existingCard, userId);
        }

        // 创建新版本
        MemoryCardVersionDO newVersion = new MemoryCardVersionDO();
        newVersion.setCardId(request.getId());
        newVersion.setVersion(currentVersion.getVersion() + 1);
        newVersion.setCreatorId(userId);
        newVersion.setFront(request.getFront());
        newVersion.setBack(request.getBack());
        newVersion.setContentHash(contentHash);
        newVersion.setIsActive(true);
        newVersion.setCreatedAt(LocalDateTime.now());

        int versionResult = cardVersionDataService.insert(newVersion);
        if (versionResult <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("创建新版本失败");
        }

        // 标记旧版本为非活跃
        cardVersionDataService.updateActiveStatus(currentVersion.getId(), false);

        // 更新卡片的当前版本ID
        MemoryCardDO card = cardDataService.getById(request.getId());
        card.setCurrentVersionId(newVersion.getId());
        card.setUpdatedAt(LocalDateTime.now());
        cardDataService.update(card);

        log.info("Updated card: {} to version: {} by user: {}", request.getId(), newVersion.getVersion(), userId);

        // 卡片内容变化，将卡片组状态设置为审核中并增加版本
        deckDataService.updateStateAndIncrementVersion(existingCard.getDeckId(), MemoryCardDeckState.PENDING.value());

        return toDTOV1(card, userId);
    }

    /**
     * 删除卡片
     */
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        // 验证并获取卡片（需要使用对象）
        userDataService.validateExists(userId);
        MemoryCardDO card = cardDataService.validateAndGet(cardId);

        // 验证权限：只有卡片创建者可以删除
        if (!card.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限删除此卡片");
        }

        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDataService.getById(card.getDeckId());
        if (deck == null) {
            throw ErrorCode.SYSTEM_ERROR.exception("卡片组不存在");
        }

        // 软删除卡片：直接修改已获取的card对象
        card.setState(MemoryCardState.DELETED.value()); // 已删除状态
        card.setUpdatedAt(LocalDateTime.now());
        cardDataService.update(card);

        // 原子操作：减少卡片数量并增加版本（保证不会小于0）
        deckDataService.decrementCardCountAndIncrementVersion(card.getDeckId());

        log.info("Deleted card: {} from deck: {} by user: {}", cardId, card.getDeckId(), userId);
    }

    /**
     * 批量删除指定卡片组中的所有卡片
     */
    @Transactional
    public void deleteCardsByDeck(Long userId, Long deckId) {
        // 验证用户和卡片组
        userDataService.validateExists(userId);
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证权限：只有卡片组创建者可以删除卡片
        if (!deck.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限删除此卡片组中的卡片");
        }

        // 获取卡片组中的所有有效卡片
        List<MemoryCardDO> cards = cardDataService.getByDeckId(deckId);
        if (cards.isEmpty()) {
            return;
        }

        // 批量软删除卡片
        LocalDateTime now = LocalDateTime.now();
        for (MemoryCardDO card : cards) {
            card.setState(MemoryCardState.DELETED.value());
            card.setUpdatedAt(now);
        }

        // 批量更新卡片状态
        cardDataService.batchUpdate(cards);

        // 重置卡片组的卡片数量为0并增加版本
        deck.setCardCount(0);
        deck.setVersion(deck.getVersion() + 1);
        deck.setUpdatedAt(now);
        deckDataService.update(deck);

        log.info("Batch deleted {} cards from deck: {} by user: {}", cards.size(), deckId, userId);
    }

    // ========== 私有方法 ==========

    /**
     * 计算内容哈希（使用SHA-256算法提高安全性）
     */
    private String calculateContentHash(String front, String back) {
        String content = (front != null ? front : "") + "|" + (back != null ? back : "");
        return Utils.hashSHA(content);
    }

}