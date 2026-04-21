package com.twicemax.memory.deck;

import com.twicemax.memory.card.MemoryCardDO;
import com.twicemax.memory.card.MemoryCardDataService;
import com.twicemax.memory.card.MemoryCardVersionDO;
import com.twicemax.memory.card.MemoryCardVersionDataService;
import com.twicemax.memory.review.UserCardSrsDO;
import com.twicemax.memory.review.UserCardSrsDataService;
import com.twicemax.shared.domain.Enums.ContentState;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 记忆卡片组领域服务
 *
 * 只依赖 memory 模块，处理卡片组的核心业务逻辑
 *
 * 职责：
 * - 卡片组的增删改查（纯业务逻辑）
 * - 卡片组状态变更（SUBMITTED → PUBLISHED → BANNED）
 * - 卡片组版本管理
 * - 卡片组内卡片的管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDeckDomainService {

    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDataService memoryCardDataService;
    private final MemoryCardVersionDataService cardVersionDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final SystemProperties systemProperties;

    // ========== Command 方法（写操作）==========

    /**
     * 创建卡片组
     *
     * @param creatorId 创建者ID
     * @param postId 源帖子ID
     * @param nodeId 节点ID
     * @param description 描述
     * @param cardCount 卡片数量
     * @return 创建的卡片组
     */
    @Transactional
    public MemoryCardDeckDO createDeck(Long creatorId, Long postId, Long nodeId, String description, Integer cardCount) {
        // 验证参数
        checkNotNull(creatorId, "创建者ID不能为空");
        checkNotNull(postId, "源帖子ID不能为空");
        checkNotNull(nodeId, "节点ID不能为空");

        // 构建卡片组DO
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setPostId(postId);
        deck.setNodeId(nodeId);
        deck.setCreatorId(creatorId);
        deck.setDescription(description);
        deck.setVersion(1);
        deck.setState(ContentState.SUBMITTED.value()); // 默认审核中
        deck.setCardCount(cardCount != null ? cardCount : 0);

        // 插入数据库
        int result = deckDataService.insert(deck);
        if (result <= 0) {
            throw StatusCode.SYSTEM_ERROR.exception("创建卡片组失败");
        }

        log.info("卡片组创建成功: deckId={}，cardCount={}，creatorId={}", deck.getId(), cardCount, creatorId);
        return deck;
    }

    /**
     * 更新卡片组
     *
     * @param deckId 卡片组ID
     * @param userId 用户ID（权限验证）
     * @param description 描述（可选）
     * @return 更新后的卡片组
     */
    @Transactional
    public MemoryCardDeckDO updateDeck(Long deckId, Long userId, String description) {
        // 获取现有卡片组
        MemoryCardDeckDO existingDeck = deckDataService.validateAndGet(deckId);

        // 验证权限：只有创建者可以修改
        if (!existingDeck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限修改此卡片组");
        }

        // 更新字段
        MemoryCardDeckDO deck = deckDataService.getById(deckId);
        if (description != null) {
            deck.setDescription(description);
        }
        deck.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        deckDataService.update(deck);

        log.info("卡片组更新成功: deckId={}，userId={}", deckId, userId);
        return deck;
    }

    /**
     * 删除卡片组（软删除）
     *
     * @param deckId 卡片组ID
     * @param userId 用户ID（权限验证）
     */
    @Transactional
    public void deleteDeck(Long deckId, Long userId) {
        if (deckId == null || deckId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("卡片组ID无效");
        }
        if (userId == null || userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID无效");
        }

        MemoryCardDeckDO deck = deckDataService.getById(deckId);
        if (deck == null) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }

        // 验证权限：只能删除自己创建的卡片组
        if (!deck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception();
        }

        int result = deckDataService.softDelete(deckId);
        if (result == 0) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }

        log.info("卡片组删除成功: deckId={}，userId={}", deckId, userId);
    }

    /**
     * 审核通过卡片组
     *
     * @param deckId 卡片组ID
     */
    @Transactional
    public void approve(Long deckId) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证状态：只有待审核的卡片组才能通过
        if (deck.getState() != ContentState.SUBMITTED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只有待审核状态的卡片组才能通过审核");
        }

        // 更新状态为正常
        deck.setState(ContentState.PUBLISHED.value());
        deck.setUpdatedAt(LocalDateTime.now());

        deckDataService.update(deck);

        log.info("卡片组审核通过: deckId={}", deckId);
    }

    /**
     * 拒绝卡片组（审核不通过）
     *
     * @param deckId 卡片组ID
     * @param reason 拒绝原因
     */
    @Transactional
    public void reject(Long deckId, String reason) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 更新状态为审核不通过
        deck.setState(ContentState.REJECTED.value());
        deck.setReason(reason);
        deck.setUpdatedAt(LocalDateTime.now());

        deckDataService.update(deck);

        log.info("卡片组审核拒绝: deckId={}，reason={}", deckId, reason);
    }

    /**
     * 封禁卡片组（违规封禁）
     *
     * @param deckId 卡片组ID
     * @param reason 封禁原因
     */
    @Transactional
    public void ban(Long deckId, String reason) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 更新状态为封禁
        deck.setState(ContentState.BANNED.value());
        deck.setReason(reason);
        deck.setUpdatedAt(LocalDateTime.now());

        deckDataService.update(deck);

        log.info("卡片组封禁: deckId={}，reason={}", deckId, reason);
    }

    /**
     * 恢复卡片组
     *
     * @param deckId 卡片组ID
     */
    @Transactional
    public void restore(Long deckId) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证状态：只有屏蔽状态的卡片组才能恢复
        if (deck.getState() != ContentState.BANNED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只有屏蔽状态的卡片组才能恢复");
        }

        // 更新状态为正常
        deck.setState(ContentState.PUBLISHED.value());
        deck.setUpdatedAt(LocalDateTime.now());

        deckDataService.update(deck);

        log.info("卡片组恢复: deckId={}", deckId);
    }

    /**
     * 整体替换卡片组
     * 更新描述并将状态改为待审核
     *
     * @param deckId 卡片组ID
     * @param userId 用户ID（权限验证）
     * @param description 新描述（可选）
     * @return 更新后的卡片组
     */
    @Transactional
    public MemoryCardDeckDO replaceAllCards(Long deckId, Long userId, String description) {
        // 验证并获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证权限：只有创建者可以替换卡片
        if (!deck.getCreatorId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权限修改此卡片组");
        }

        // 更新卡片组信息（如果提供了）
        if (description != null) {
            deck.setDescription(description);
        }

        // 设置为待审核状态
        deck.setState(ContentState.SUBMITTED.value());
        deck.setUpdatedAt(LocalDateTime.now());
        deckDataService.update(deck);

        log.info("卡片组全部卡片替换成功: deckId={}，userId={}", deckId, userId);
        return deck;
    }

    // ========== Query 方法（读操作）==========

    /**
     * 根据ID获取卡片组
     */
    public MemoryCardDeckDO getById(Long deckId) {
        return deckDataService.getById(deckId);
    }

    /**
     * 验证并获取卡片组
     */
    public MemoryCardDeckDO validateAndGet(Long deckId) {
        return deckDataService.validateAndGet(deckId);
    }

    /**
     * 根据帖子ID获取卡片组列表 - 动态排序和分页
     * sortBy=createdAt: 按ID降序
     * sortBy=score: 按分数降序
     */
    public List<MemoryCardDeckDO> getListByPostDynamic(Long postId, ContentState state, String sortBy, Double lastScore, Long lastId, Integer limit) {
        return deckDataService.getListByPostDynamic(postId, state.value(), sortBy, lastScore, lastId, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByPostAndCreator(Long postId, Long creatorId, ContentState state, Integer limit) {
        return deckDataService.getListByPostAndCreator(postId, creatorId, state.value(), limit);
    }

    /**
     * 根据帖子和创建者获取所有状态的卡片组列表
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorAllStates(Long postId, Long creatorId, Integer limit) {
        return deckDataService.getListByPostAndCreatorAllStates(postId, creatorId, limit);
    }

    /**
     * 根据帖子和创建者获取所有状态的卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPagingAllStates(Long postId, Long creatorId, Long lastId, Integer limit) {
        return deckDataService.getListByPostAndCreatorWithIdPagingAllStates(postId, creatorId, lastId, limit);
    }

    /**
     * 根据帖子和创建者获取所有状态的卡片组列表 - 动态排序和分页
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorDynamicAllStates(Long postId, Long creatorId, String sortBy, Double lastScore, Long lastId, Integer limit) {
        return deckDataService.getListByPostAndCreatorDynamicAllStates(postId, creatorId, sortBy, lastScore, lastId, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByCreatorWithIdPaging(Long creatorId, Long lastId, Integer limit) {
        return deckDataService.getListByCreatorWithIdPaging(creatorId, lastId, limit);
    }

    /**
     * 根据节点ID获取卡片组列表
     */
    public List<MemoryCardDeckDO> getListByNode(Long nodeId, Integer state, Integer limit) {
        return deckDataService.getListByNode(nodeId, state, limit);
    }

    /**
     * 根据节点ID获取卡片组列表 - Keyset分页
     */
    public List<MemoryCardDeckDO> getListByNodeKeyset(Long nodeId, Double lastScore, Long lastId, Integer state, Integer limit) {
        return deckDataService.getListByNodeKeyset(nodeId, lastScore, lastId, state, limit);
    }

    /**
     * 根据状态获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> listByState(ContentState state, Long lastId, Integer limit) {
        return deckDataService.listByState(state.value(), lastId, limit);
    }

    /**
     * 根据帖子获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByPostWithIdPaging(Long postId, ContentState state, Long lastId, Integer limit) {
        return deckDataService.getListByPostWithIdPaging(postId, state.value(), lastId, limit);
    }

    /**
     * 根据帖子获取卡片组列表 - 用于审核
     */
    public List<MemoryCardDeckDO> getListByPostForReview(Long postId, ContentState state, Integer limit) {
        return deckDataService.getListByPostForReview(postId, state.value(), limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorWithIdPaging(Long postId, Long creatorId, ContentState state, Long lastId, Integer limit) {
        return deckDataService.getListByPostAndCreatorWithIdPaging(postId, creatorId, state.value(), lastId, limit);
    }

    /**
     * 根据帖子和创建者获取卡片组列表 - 用于审核
     */
    public List<MemoryCardDeckDO> getListByPostAndCreatorForReview(Long postId, Long creatorId, ContentState state, Integer limit) {
        return deckDataService.getListByPostAndCreatorForReview(postId, creatorId, state.value(), limit);
    }

    /**
     * 根据创建者和状态获取卡片组列表 - ID分页
     */
    public List<MemoryCardDeckDO> getListByCreatorWithIdPagingAndState(Long creatorId, ContentState state, Long lastId, Integer limit) {
        return deckDataService.getListByCreatorWithIdPagingAndState(creatorId, state.value(), lastId, limit);
    }

    /**
     * 根据创建者获取卡片组列表 - 用于审核
     */
    public List<MemoryCardDeckDO> getListByCreatorForReview(Long creatorId, ContentState state, Integer limit) {
        return deckDataService.getListByCreatorForReview(creatorId, state.value(), limit);
    }

    // ========== 版本管理方法 ==========

    /**
     * 获取卡片组更新差异
     *
     * @param deckId 卡片组ID
     * @param userCurrentVersion 用户当前版本（暂未使用）
     * @param userId 用户ID
     * @return 差异信息
     */
    public Map<String, Object> getDeckDiff(Long deckId, Integer userCurrentVersion, Long userId) {
        // 获取当前版本的卡片组
        MemoryCardDeckDO currentDeck = deckDataService.validateAndGet(deckId);

        // 获取当前所有卡片
        List<MemoryCardDO> currentCards = memoryCardDataService.getByDeckId(deckId);
        Set<Long> currentCardIds = currentCards.stream()
            .map(MemoryCardDO::getId)
            .collect(Collectors.toSet());

        // 获取用户在这个deck下的所有学习记录
        List<UserCardSrsDO> userStates = userCardSrsDataService.getByUserAndDeckId(userId, deckId);
        Set<Long> userStudiedCardIds = userStates.stream()
            .map(UserCardSrsDO::getCardId)
            .collect(Collectors.toSet());

        Map<String, Object> diffResult = new HashMap<>();
        diffResult.put("deckId", deckId);
        diffResult.put("currentVersion", currentDeck.getVersion());
        diffResult.put("description", currentDeck.getDescription());

        List<Map<String, Object>> cardDiffs = new ArrayList<>();
        int addedCount = 0;
        int modifiedCount = 0;
        int deletedCount = 0;

        // 批量获取卡片版本内容
        Set<Long> versionIds = currentCards.stream()
            .map(MemoryCardDO::getCurrentVersionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 获取用户学习记录中的版本ID
        Set<Long> userVersionIds = userStates.stream()
            .map(UserCardSrsDO::getCardVersionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        versionIds.addAll(userVersionIds);
        Map<Long, MemoryCardVersionDO> versionMap = cardVersionDataService.getMapByIds(versionIds);

        // 检查新增和修改的卡片
        for (MemoryCardDO card : currentCards) {
            Map<String, Object> cardDiff = new HashMap<>();
            cardDiff.put("cardId", card.getId());

            if (!userStudiedCardIds.contains(card.getId())) {
                // 新增的卡片
                cardDiff.put("type", "added");

                // 获取卡片内容
                MemoryCardVersionDO version = versionMap.get(card.getCurrentVersionId());
                if (version != null) {
                    Map<String, Object> newVersion = new HashMap<>();
                    newVersion.put("front", version.getFront());
                    newVersion.put("back", version.getBack());
                    cardDiff.put("newVersion", newVersion);
                }
                addedCount++;
                cardDiffs.add(cardDiff);
            } else {
                // 检查卡片是否真正有变化
                UserCardSrsDO userState = userStates.stream()
                    .filter(state -> state.getCardId().equals(card.getId()))
                    .findFirst()
                    .orElse(null);

                // 只有当卡片版本实际发生变化时才标记为修改
                if (userState != null && userState.getCardVersionId() != null &&
                    !userState.getCardVersionId().equals(card.getCurrentVersionId())) {
                    // 修改的卡片
                    cardDiff.put("type", "modified");

                    // 获取当前版本内容
                    MemoryCardVersionDO currentVersion = versionMap.get(card.getCurrentVersionId());
                    if (currentVersion != null) {
                        Map<String, Object> newVersion = new HashMap<>();
                        newVersion.put("front", currentVersion.getFront());
                        newVersion.put("back", currentVersion.getBack());
                        cardDiff.put("newVersion", newVersion);
                    }

                    // 获取用户学习时的版本内容
                    MemoryCardVersionDO oldVersion = versionMap.get(userState.getCardVersionId());
                    if (oldVersion != null) {
                        Map<String, Object> oldVersionMap = new HashMap<>();
                        oldVersionMap.put("front", oldVersion.getFront());
                        oldVersionMap.put("back", oldVersion.getBack());
                        cardDiff.put("oldVersion", oldVersionMap);
                    }

                    modifiedCount++;
                    cardDiffs.add(cardDiff);
                }
                // 如果卡片版本没有变化，则不添加到diff结果中
            }
        }

        // 检查删除的卡片（用户之前学过但现在不存在的）
        for (Long studiedCardId : userStudiedCardIds) {
            if (!currentCardIds.contains(studiedCardId)) {
                Map<String, Object> cardDiff = new HashMap<>();
                cardDiff.put("cardId", studiedCardId);
                cardDiff.put("type", "deleted");

                // 获取删除的卡片内容（从用户学习记录的版本快照获取）
                UserCardSrsDO userState = userStates.stream()
                    .filter(state -> state.getCardId().equals(studiedCardId))
                    .findFirst()
                    .orElse(null);

                if (userState != null && userState.getCardVersionId() != null) {
                    MemoryCardVersionDO oldVersion = versionMap.get(userState.getCardVersionId());
                    if (oldVersion != null) {
                        Map<String, Object> oldVersionMap = new HashMap<>();
                        oldVersionMap.put("front", oldVersion.getFront());
                        oldVersionMap.put("back", oldVersion.getBack());
                        cardDiff.put("oldVersion", oldVersionMap);
                    }
                }

                deletedCount++;
                cardDiffs.add(cardDiff);
            }
        }

        diffResult.put("cardDiffs", cardDiffs);

        Map<String, Object> summary = new HashMap<>();
        summary.put("addedCount", addedCount);
        summary.put("modifiedCount", modifiedCount);
        summary.put("deletedCount", deletedCount);
        diffResult.put("summary", summary);

        return diffResult;
    }

    /**
     * 接受卡片组更新
     *
     * @param deckId 卡片组ID
     * @param cardIds 要接受的卡片ID列表（空表示接受所有）
     * @param userId 用户ID
     * @param nodeId 节点ID（用于创建新的SRS记录）
     * @param courseId 当前浏览的课程ID（可选，用于创建 user_card_in_course 记录）
     * @param removeOtherDeckCards 是否删除该节点下来自其他卡片组的卡片
     */
    @Transactional
    public void acceptDeckChanges(Long deckId, List<Long> cardIds, Long userId, Long nodeId, Long courseId, boolean removeOtherDeckCards) {
        // 验证卡片组存在
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");

        // 如果需要删除其他卡片组的卡片，先处理
        if (removeOtherDeckCards) {
            int deletedCount = userCardSrsDataService.deleteByUserAndNodeExcludeDeck(userId, nodeId, deckId);
            if (deletedCount > 0) {
                log.info("卡片组删除其他来源卡片: userId={}，nodeId={}，deletedCount={}", userId, nodeId, deletedCount);
            }
        }

        // 获取用户在这个deck下的所有学习记录
        List<UserCardSrsDO> userStates = userCardSrsDataService.getByUserAndDeckId(userId, deckId);
        Map<Long, UserCardSrsDO> userStateMap = userStates.stream()
            .collect(Collectors.toMap(UserCardSrsDO::getCardId, state -> state));

        // 获取当前deck中的所有有效卡片
        List<MemoryCardDO> currentCards = memoryCardDataService.getByDeckId(deckId);
        Map<Long, MemoryCardDO> currentCardMap = currentCards.stream()
            .collect(Collectors.toMap(MemoryCardDO::getId, card -> card));

        // 计算将要新增的卡片数量
        long newCardCount = currentCards.stream()
            .filter(card -> !userStateMap.containsKey(card.getId()))
            .count();

        // 检查用户卡片总数限制
        if (newCardCount > 0) {
            int maxCardsPerUser = systemProperties.getSrs().getMaxCardsPerUser();
            long userCardCount = userCardSrsDataService.countByUser(userId);
            if (userCardCount + newCardCount > maxCardsPerUser) {
                throw StatusCode.USER_CARD_LIMIT_EXCEEDED.exception(
                    String.format("您已有%d张卡片，添加%d张新卡片将超过%d张的限制",
                        userCardCount, newCardCount, maxCardsPerUser)
                );
            }
        }

        // 准备新增卡片的SRS状态列表和卡片ID列表
        List<UserCardSrsDO> newSrsStates = new ArrayList<>();
        List<Long> newCardIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (cardIds.isEmpty()) {
            // 如果没有指定卡片，则接受所有更新

            // 1. 处理现有学习记录
            for (UserCardSrsDO userState : userStates) {
                MemoryCardDO card = currentCardMap.get(userState.getCardId());
                if (card != null) {
                    // 存在的卡片：更新版本快照
                    userState.setDeckVersion(deck.getVersion());
                    userState.setCardVersionId(card.getCurrentVersionId());
                    userState.setUpdatedAt(now);
                    userCardSrsDataService.update(userState);
                } else {
                    // 已删除的卡片：删除学习记录
                    userCardSrsDataService.deleteByUserAndCard(userId, userState.getCardId());
                    log.info("卡片组删除已移除卡片的SRS状态: cardId={}，userId={}", userState.getCardId(), userId);
                }
            }

            // 2. 处理新增卡片
            for (MemoryCardDO card : currentCards) {
                if (!userStateMap.containsKey(card.getId())) {
                    // 新增的卡片：创建SRS学习记录
                    UserCardSrsDO newState = userCardSrsDataService.createNewSrsState(
                        userId, card.getId(), deck.getId(), nodeId, courseId, deck.getVersion(), card.getCurrentVersionId());
                    newSrsStates.add(newState);
                    newCardIds.add(card.getId());
                }
            }

        } else {
            // 只处理指定的卡片，但所有学习记录的deck_version都要更新

            // 1. 先更新所有学习记录的deck_version（表示用户已经处理过这个版本）
            for (UserCardSrsDO userState : userStates) {
                MemoryCardDO card = currentCardMap.get(userState.getCardId());
                if (card != null) {
                    // 存在的卡片：至少更新deck_version
                    userState.setDeckVersion(deck.getVersion());

                    // 如果是选中的卡片，还要更新card_version_id
                    if (cardIds.contains(userState.getCardId())) {
                        userState.setCardVersionId(card.getCurrentVersionId());
                    }

                    userState.setUpdatedAt(now);
                    userCardSrsDataService.update(userState);
                } else {
                    // 已删除的卡片：如果在选中列表中，则删除学习记录
                    if (cardIds.contains(userState.getCardId())) {
                        userCardSrsDataService.deleteByUserAndCard(userId, userState.getCardId());
                        log.info("卡片组删除已移除卡片的SRS状态: cardId={}，userId={}", userState.getCardId(), userId);
                    }
                }
            }

            // 2. 处理选中的新增卡片
            for (Long cardId : cardIds) {
                if (!userStateMap.containsKey(cardId)) {
                    MemoryCardDO card = currentCardMap.get(cardId);
                    if (card != null) {
                        // 新增的卡片：创建SRS学习记录
                        UserCardSrsDO newState = userCardSrsDataService.createNewSrsState(
                            userId, card.getId(), deck.getId(), nodeId, courseId, deck.getVersion(), card.getCurrentVersionId());
                        newSrsStates.add(newState);
                        newCardIds.add(card.getId());
                    }
                }
            }
        }

        // 批量插入新增卡片的SRS状态（已包含courseId）
        if (!newSrsStates.isEmpty()) {
            userCardSrsDataService.batchInsertIgnoreSrsStates(newSrsStates);
            log.info("卡片组创建新增卡片SRS状态: count={}，userId={}，courseId={}",
                    newSrsStates.size(), userId, courseId);
        }

        log.info("卡片组接受更新: userId={}，deckId={}，targetItems={}",
                userId, deckId, cardIds.isEmpty() ? userStates.size() + newSrsStates.size() : cardIds.size());
    }

    /**
     * 移动节点到课程
     * 将用户在指定节点下学习的所有卡片移动到指定课程
     *
     * @param userId 用户ID
     * @param nodeId 节点ID
     * @param courseId 目标课程ID
     * @return 移动的卡片数量
     */
    @Transactional
    public int moveNodeToCourse(Long userId, Long nodeId, Long courseId) {
        checkNotNull(userId, "用户ID不能为空");
        checkNotNull(nodeId, "节点ID不能为空");
        checkNotNull(courseId, "课程ID不能为空");

        int movedCount = userCardSrsDataService.updateCourseIdByUserAndNode(userId, nodeId, courseId);
        log.info("卡片组移动节点到课程: userId={}，nodeId={}，courseId={}，movedCount={}", userId, nodeId, courseId, movedCount);
        return movedCount;
    }
}
