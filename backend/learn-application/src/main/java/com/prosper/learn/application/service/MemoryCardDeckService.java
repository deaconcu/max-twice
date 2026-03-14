package com.prosper.learn.application.service;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.application.assembler.CardAssembler;
import com.prosper.learn.application.assembler.DeckAssembler;
import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.response.deck.*;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.service.robot.PostQueueService;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentApprovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentBannedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRejectedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRemovedEvent;
import com.prosper.learn.shared.domain.event.content.lifecycle.ContentRestoredEvent;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.*;

/**
 * 记忆卡片组应用服务
 *
 * 负责协调跨领域逻辑、事件发布、DTO转换
 *
 * 核心功能：
 * - 跨域数据聚合（User + Post + Node + Course + Upvote）
 * - DTO转换（DO → DTO with Creator/Vote/Course）
 * - 事件发布（ContentApprovedEvent、ContentRejectedEvent）
 * - 复杂查询编排（审核列表、用户卡片组列表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDeckService {

    // 领域服务
    private final MemoryCardDeckDomainService deckDomainService;

    // 跨域服务依赖
    private final MemoryCardDataService memoryCardDataService;
    private final UserDataService userDataService;
    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final MemoryCardService memoryCardService;
    private final ScoreCalculationService scoreCalculationService;
    private final PostQueueService postQueueService;
    private final ContentStatsDataService contentStatsDataService;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

    // DTO 组装器和转换器
    private final DeckAssembler deckAssembler;
    private final CardAssembler cardAssembler;
    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;

    // ========= 业务方法(Query) ==========

    /**
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
     */
    public KeysetPageResponse<DeckFullDTO> getPostPublicDecks(
            Long postId, String sortBy, Double lastScore, Long lastId, Integer limit, Long userId) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        // 使用动态方法，Mapper 根据 sortBy 和分页参数自动选择排序方式
        List<MemoryCardDeckDO> deckList = deckDomainService.getListByPostDynamic(
            postId, Enums.ContentState.PUBLISHED, sortBy, lastScore, lastId, limit + 1);

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 需求2: 获取帖子创建者提交的卡片组 - 固定按ID降序
     */
    public KeysetPageResponse<DeckFullDTO> getPostCreatorDeck(
            Long postId, Long lastId, Integer limit, Long userId) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 1;
        if (limit > 50) limit = 50;

        PostDO post = postDataService.validateAndGet(postId);
        Long postCreatorId = post.getCreatorId();

        List<MemoryCardDeckDO> deckList;
        if (!postCreatorId.equals(userId)) {
            // post创建者不是当前用户，只查询normal状态，按ID降序
            if (lastId != null) {
                deckList = deckDomainService.getListByPostAndCreatorWithIdPaging(
                        postId, postCreatorId, Enums.ContentState.PUBLISHED, lastId, limit + 1);
            } else {
                // 首次查询，没有 lastId
                deckList = deckDomainService.getListByPostAndCreator(
                        postId, postCreatorId, Enums.ContentState.PUBLISHED, limit + 1);
            }
        } else {
            // post创建者就是当前用户，查询所有状态，按ID降序
            if (lastId != null) {
                deckList = deckDomainService.getListByPostAndCreatorWithIdPagingAllStates(
                        postId, postCreatorId, lastId, limit + 1);
            } else {
                deckList = deckDomainService.getListByPostAndCreatorAllStates(
                        postId, postCreatorId, limit + 1);
            }
        }

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 需求3: 获取用户自己在指定帖子下提交的卡片组
     */
    public KeysetPageResponse<DeckFullDTO> getMyPostDeck(
            Long postId, Long userId, String sortBy, Double lastScore, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 1;
        if (limit > 50) limit = 50;

        // 查询所有状态的卡片组
        List<MemoryCardDeckDO> deckList = deckDomainService.getListByPostAndCreatorDynamicAllStates(
                postId, userId, sortBy, lastScore, lastId, limit + 1);

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 需求4: 获取用户自己提交的所有卡片组 - keyset分页，全部状态
     */
    /**
     * 获取用户的卡片组列表（按ID逆序，返回所有状态）
     * @param userId 用户ID
     * @param currentUserId 当前登录用户ID（用于获取点赞状态）
     * @param lastId 最后一条记录的ID（用于分页）
     * @param limit 每页数量
     */
    public KeysetPageResponse<DeckFullDTO> getUserDecks(
            Long userId, Long currentUserId, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;
        // 使用 ID 分页查询
        if (lastId != null) {
            deckList = deckDomainService.getListByCreatorWithIdPaging(userId, lastId, limit + 1);
        } else {
            deckList = deckDomainService.getListByCreator(userId, limit + 1);
        }

        return buildDeckResponse(deckList, limit, currentUserId);
    }

    /**
     * 构建卡片组响应的通用方法
     */
    private KeysetPageResponse<DeckFullDTO> buildDeckResponse(
            List<MemoryCardDeckDO> deckList, Integer limit, Long userId) {
        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息和点赞状态)
        List<DeckFullDTO> dtoList = deckAssembler.toFullDTO(deckList, userId);

        // 构建响应
        KeysetPageResponse<DeckFullDTO> response = new KeysetPageResponse<>();
        response.setItems(dtoList);
        response.setHasMore(hasMore);

        if (hasMore && !deckList.isEmpty()) {
            MemoryCardDeckDO lastDeck = deckList.get(deckList.size() - 1);
            KeysetPageResponse.NextCursor nextCursor = new KeysetPageResponse.NextCursor();
            nextCursor.setLastScore(lastDeck.getScore());
            nextCursor.setLastId(lastDeck.getId());
            response.setNextCursor(nextCursor);
        }

        return response;
    }

    /**
     * 根据节点ID获取卡片组列表 - Keyset分页
     */
    public KeysetPageResponse<DeckFullDTO> getDecksByNode(
            Long nodeId, Double lastScore, Long lastId, Integer limit, Long userId) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 20;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;

        // 只查询正常状态的卡片组
        int state = Enums.ContentState.PUBLISHED.value();

        if (lastScore != null && lastId != null) {
            deckList = deckDomainService.getListByNodeKeyset(nodeId, lastScore, lastId, state, limit + 1);
        } else {
            deckList = deckDomainService.getListByNode(nodeId, state, limit + 1);
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息和点赞状态)
        List<DeckFullDTO> dtoList = deckAssembler.toFullDTO(deckList, userId);

        // 构建响应
        KeysetPageResponse<DeckFullDTO> response = new KeysetPageResponse<>();
        response.setItems(dtoList);
        response.setHasMore(hasMore);

        if (hasMore && !deckList.isEmpty()) {
            MemoryCardDeckDO lastDeck = deckList.get(deckList.size() - 1);
            KeysetPageResponse.NextCursor nextCursor = new KeysetPageResponse.NextCursor();
            nextCursor.setLastScore(lastDeck.getScore());
            nextCursor.setLastId(lastDeck.getId());
            response.setNextCursor(nextCursor);
        }

        return response;
    }

    // ========== Admin 管理接口 ==========

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Admin管理：按状态查询卡片组列表
     */
    public KeysetPageResponse<DeckAdminDTO> listByState(Enums.ContentState state, Long lastId) {
        List<MemoryCardDeckDO> deckList = deckDomainService.listByState(
                state != null ? state : Enums.ContentState.SUBMITTED,
                lastId,
                DEFAULT_PAGE_SIZE + 1);

        boolean hasMore = deckList.size() > DEFAULT_PAGE_SIZE;
        if (hasMore) {
            deckList = deckList.subList(0, DEFAULT_PAGE_SIZE);
        }

        List<DeckAdminDTO> items = deckConverter.toAdminDTO(deckList);
        fillAdminDTOInfo(deckList, items);

        Long nextLastId = hasMore && !items.isEmpty() ? items.get(items.size() - 1).getId() : null;

        return KeysetPageResponse.of(items, hasMore, null, nextLastId);
    }

    /**
     * 批量填充 DeckAdminDTO 的关联信息
     */
    private void fillAdminDTOInfo(List<MemoryCardDeckDO> deckList, List<DeckAdminDTO> items) {
        if (items.isEmpty()) {
            return;
        }

        // 收集 creatorId
        Set<Long> creatorIds = deckList.stream()
                .map(MemoryCardDeckDO::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询 creator
        Map<Long, UserDO> creatorMap = creatorIds.isEmpty() ? Map.of() :
                userDataService.getMapByIds(creatorIds);

        // 创建 deckDO 映射
        Map<Long, MemoryCardDeckDO> deckDOMap = deckList.stream()
                .collect(Collectors.toMap(MemoryCardDeckDO::getId, d -> d));

        // 填充 creator 信息
        for (DeckAdminDTO dto : items) {
            MemoryCardDeckDO deckDO = deckDOMap.get(dto.getId());
            if (deckDO != null && deckDO.getCreatorId() != null) {
                UserDO userDO = creatorMap.get(deckDO.getCreatorId());
                if (userDO != null) {
                    dto.setCreator(userConverter.toBriefDTO(userDO));
                }
            }
        }

        // 批量填充统计数据
        List<Long> deckIds = deckList.stream().map(MemoryCardDeckDO::getId).collect(Collectors.toList());
        if (!deckIds.isEmpty()) {
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(Enums.ContentType.memory_card_deck, deckIds);
            Map<Long, ContentStatsDO> statsMap = statsList.stream()
                    .collect(Collectors.toMap(ContentStatsDO::getContentId, s -> s));
            for (DeckAdminDTO dto : items) {
                ContentStatsDO stats = statsMap.get(dto.getId());
                if (stats != null) {
                    dto.setViewCount(stats.getViewCount());
                    dto.setBookmarkCount(stats.getBookmarkCount());
                    dto.setRejectCount(stats.getRejectCount());
                }
            }
        }
    }

    /**
     * 获取卡片组审核列表 - 包含卡片内容
     */
    public KeysetPageResponse<DeckAndCardsDTO> getDecksForReview(
            Long postId, Long creatorId, Integer state, Long lastId, Long userId) {

        // 固定每页数量
        int limit = 20;

        // 默认查询待审核状态的卡片组
        Enums.ContentState defaultState =
                state != null ? Enums.ContentState.getByValue(state.byteValue()): Enums.ContentState.SUBMITTED;

        List<MemoryCardDeckDO> deckList;

        // 根据查询条件组合获取数据 - 统一使用ID分页
        if (postId != null && creatorId != null) {
            // 同时按帖子和创建者查询
            if (lastId != null) {
                deckList = deckDomainService.getListByPostAndCreatorWithIdPaging(postId, creatorId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDomainService.getListByPostAndCreatorForReview(postId, creatorId, defaultState, limit + 1);
            }
        } else if (postId != null) {
            // 只按帖子查询
            if (lastId != null) {
                deckList = deckDomainService.getListByPostWithIdPaging(postId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDomainService.getListByPostForReview(postId, defaultState, limit + 1);
            }
        } else if (creatorId != null) {
            // 只按创建者查询
            if (lastId != null) {
                deckList = deckDomainService.getListByCreatorWithIdPagingAndState(creatorId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDomainService.getListByCreatorForReview(creatorId, defaultState, limit + 1);
            }
        } else {
            // 按状态查询，管理员审核页面 - 统一使用 listByState（lastId 为 null 时查询第一页）
            deckList = deckDomainService.listByState(defaultState, lastId, limit + 1);
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为基础DTO列表(包含创建者信息和点赞状态)
        List<DeckFullDTO> baseDTOList = deckAssembler.toFullDTO(deckList, userId);
        
        // 批量获取所有卡片组的卡片信息
        Set<Long> deckIds = deckList.stream().map(MemoryCardDeckDO::getId).collect(Collectors.toSet());
        Map<Long, List<CardWithSrsDTO>> deckCardsMap = new HashMap<>();
        
        if (!deckIds.isEmpty()) {
            // 直接从数据层批量查询卡片
            List<MemoryCardDO> allCards = memoryCardDataService.getByDeckIds(new ArrayList<>(deckIds));
            
            // 按deckId分组
            Map<Long, List<MemoryCardDO>> cardsByDeck = allCards.stream()
                .collect(Collectors.groupingBy(MemoryCardDO::getDeckId));
            
            // 转换为DTO
            cardsByDeck.forEach((deckId, cards) -> {
                List<CardWithSrsDTO> cardViews = cardAssembler.toCardViewWithSrs(cards, null);
                deckCardsMap.put(deckId, cardViews);
            });
        }
        
        // 转换为DetailDTO并填充卡片信息
        List<DeckAndCardsDTO> dtoList = baseDTOList.stream()
            .map(baseDTO -> {
                // 转换为详情DTO
                DeckAndCardsDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
                
                // 从map中获取卡片列表
                List<CardWithSrsDTO> cards = deckCardsMap.getOrDefault(baseDTO.getId(), new ArrayList<>());
                detail.setCards(cards);
                
                return detail;
            })
            .collect(Collectors.toList());

        // 构建响应
        KeysetPageResponse<DeckAndCardsDTO> response = new KeysetPageResponse<>();
        response.setItems(dtoList);
        response.setHasMore(hasMore);

        if (hasMore && !deckList.isEmpty()) {
            MemoryCardDeckDO lastDeck = deckList.get(deckList.size() - 1);
            KeysetPageResponse.NextCursor nextCursor = new KeysetPageResponse.NextCursor();
            nextCursor.setLastId(lastDeck.getId());
            response.setNextCursor(nextCursor);
        }

        return response;
    }

    /**
     * 获取卡片组详情
     */
    public DeckAndCardsDTO getDeckDetail(Long deckId, Long userId) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.validateAndGet(deckId);

        // 先转换为基础DTO(包含创建者信息)
        DeckFullDTO baseDTO = deckAssembler.toFullDTO(deck);
        
        // 使用converter转换为详情DTO
        DeckAndCardsDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
        
        // 获取卡片列表（所有卡片tab - 不包含用户学习状态，显示最新版本）
        List<CardWithSrsDTO> cards = memoryCardService.getCardsByDeck(deckId, null);
        detail.setCards(cards);
        
        // 获取统计信息
        //DeckStatsDTO stats = calculateDeckStats(deckId);
        //detail.setStats(stats);
        
        return detail;
    }

    // ========== 业务方法(Command) ==========

    /**
     * 创建卡片组
     */
    @Transactional
    public DeckFullDTO createDeck(Long userId, CreateDeckRequest request) {
        // 验证参数
        checkNotNull(request);

        // 验证用户存在性（跨域查询）
        userDataService.validateExists(userId);

        // 确定 postId 和 nodeId
        Long postId = request.getSourcePostId();
        Long nodeId;

        if (postId != null && postId > 0) {
            // 有来源文章：从帖子获取 nodeId
            PostDO post = postDataService.validateAndGet(postId);
            nodeId = post.getNodeId();
        } else {
            // 无来源文章：postId 设为 0，nodeId 从请求中获取
            postId = 0L;
            nodeId = request.getNodeId();
            checkNotNull(nodeId, "无来源文章时，节点ID不能为空");
            nodeDataService.validateAndGet(nodeId);
        }

        // 调用 DomainService 创建卡片组
        MemoryCardDeckDO deck = deckDomainService.createDeck(
            userId,
            postId,
            nodeId,
            request.getDescription(),
            request.getCards() != null ? request.getCards().size() : 0
        );

        // 如果有卡片数据，批量创建卡片（调用 MemoryCardService）
        if (request.getCards() != null && !request.getCards().isEmpty()) {
            memoryCardService.batchCreateCards(userId, deck.getId(), request.getCards());
            log.info("Created deck {} with {} cards", deck.getId(), request.getCards().size());
        }

        // 转换并返回（包含创建者信息）
        return deckAssembler.toFullDTO(deck);
    }

    /**
     * 更新卡片组
     */
    @Transactional
    public void updateDeck(Long userId, Long deckId, String description) {
        // 调用 DomainService 更新卡片组
        deckDomainService.updateDeck(
            deckId,
            userId,
            description
        );
    }

    /**
     * 审核通过卡片组
     */
    @Transactional
    public void approve(Long deckId, Long auditorId) {
        // 调用 DomainService 执行审核通过
        deckDomainService.approve(deckId);

        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);

        // 获取帖子和节点信息用于事件（跨域查询）
        PostDO postDO = postDataService.getById(deck.getPostId());
        Long nodeId = null;
        String postContentPreview = "";
        if (postDO != null) {
            nodeId = postDO.getNodeId();
            if (postDO.getContent() != null) {
                postContentPreview = Utils.stripFormatting(postDO.getContent());
                if (postContentPreview.length() > 50) {
                    postContentPreview = postContentPreview.substring(0, 50) + "...";
                }
            }
        }

        // 发布审核通过事件，触发统计更新（不发送消息）
        eventPublisher.publishEvent(ContentApprovedEvent.forMemoryCardDeck(
            deck.getCreatorId(),
            deck.getId(),
            deck.getPostId(),
            postContentPreview,
            nodeId  // NEW: 需要更新 node 统计
        ));

        log.info("Deck {} approved by user {}", deckId, auditorId);
    }

    /**
     * 拒绝卡片组（审核不通过）
     */
    @Transactional
    public void reject(Long deckId, Long auditorId, String reason) {
        // 调用 DomainService 执行拒绝
        deckDomainService.reject(deckId, reason);

        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);

        // 获取帖子信息用于通知（跨域查询）
        PostDO postDO = postDataService.getById(deck.getPostId());
        String postContentPreview = "";
        if (postDO != null && postDO.getContent() != null) {
            postContentPreview = Utils.stripFormatting(postDO.getContent());
            if (postContentPreview.length() > 50) {
                postContentPreview = postContentPreview.substring(0, 50) + "...";
            }
        }

        // 发布审核拒绝事件，触发消息通知
        eventPublisher.publishEvent(ContentRejectedEvent.forMemoryCardDeck(
            deck.getCreatorId(),
            deck.getId(),
            deck.getPostId(),
            postContentPreview,
            reason
        ));

        log.info("Deck {} rejected by user {}, reason: {}", deckId, auditorId, reason);
    }

    /**
     * 封禁卡片组（违规封禁）
     */
    @Transactional
    public void ban(Long deckId, Long auditorId, String reason) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);
        if (deck == null) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }

        // 记录之前的状态
        Byte previousState = deck.getState();

        // 获取帖子和节点信息用于事件（跨域查询）
        PostDO postDO = postDataService.getById(deck.getPostId());
        Long nodeId = null;
        String postContentPreview = "";
        if (postDO != null) {
            nodeId = postDO.getNodeId();
            if (postDO.getContent() != null) {
                postContentPreview = Utils.stripFormatting(postDO.getContent());
                if (postContentPreview.length() > 50) {
                    postContentPreview = postContentPreview.substring(0, 50) + "...";
                }
            }
        }

        // 调用 DomainService 执行封禁
        deckDomainService.ban(deckId, reason);

        // 发布内容封禁事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentBannedEvent.forMemoryCardDeck(
            deck.getCreatorId(),
            deck.getId(),
            previousState,
            deck.getPostId(),
            nodeId,
            postContentPreview,
            reason
        ));

        log.info("Deck {} banned by user {}, reason: {}", deckId, auditorId, reason);
    }

    /**
     * 下架卡片组（已发布内容违规，降级为REJECTED状态）
     */
    @Transactional
    public void remove(Long deckId, Long auditorId, String reason) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);
        if (deck == null) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }

        // 检查状态：只能下架已发布的内容
        if (deck.getState() != Enums.ContentState.PUBLISHED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能下架已发布的内容");
        }

        // 获取帖子和节点信息用于事件（跨域查询）
        PostDO postDO = postDataService.getById(deck.getPostId());
        Long nodeId = null;
        String postContentPreview = "";
        if (postDO != null) {
            nodeId = postDO.getNodeId();
            if (postDO.getContent() != null) {
                postContentPreview = Utils.stripFormatting(postDO.getContent());
                if (postContentPreview.length() > 50) {
                    postContentPreview = postContentPreview.substring(0, 50) + "...";
                }
            }
        }

        // 调用 DomainService 执行下架（设置为REJECTED状态）
        deckDomainService.reject(deckId, reason);

        // 发布内容下架事件，触发统计更新和消息通知
        eventPublisher.publishEvent(ContentRemovedEvent.forMemoryCardDeck(
            deck.getCreatorId(),
            deck.getId(),
            deck.getPostId(),
            nodeId,
            postContentPreview,
            reason
        ));

        log.info("Deck {} removed by user {}, reason: {}", deckId, auditorId, reason);
    }

    /**
     * 恢复卡片组（管理员撤销误操作）
     */
    @Transactional
    public void restoreDeck(Long deckId, Long auditorId, String reason) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);
        if (deck == null) {
            throw StatusCode.MEMORY_CARD_DECK_NOT_FOUND.exception();
        }

        // 记录之前的状态
        Byte previousState = deck.getState();

        // 检查状态：只能恢复 REJECTED 或 BANNED 的内容
        if (previousState != Enums.ContentState.REJECTED.value() && previousState != Enums.ContentState.BANNED.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("只能恢复被拒绝或被封禁的内容");
        }

        // 获取帖子和节点信息用于事件（跨域查询）
        PostDO postDO = postDataService.getById(deck.getPostId());
        Long nodeId = null;
        String postContentPreview = "";
        if (postDO != null) {
            nodeId = postDO.getNodeId();
            if (postDO.getContent() != null) {
                postContentPreview = Utils.stripFormatting(postDO.getContent());
                if (postContentPreview.length() > 50) {
                    postContentPreview = postContentPreview.substring(0, 50) + "...";
                }
            }
        }

        // 调用 DomainService 执行恢复
        deckDomainService.restore(deckId);

        // 发布内容恢复事件，触发统计恢复和消息通知
        eventPublisher.publishEvent(ContentRestoredEvent.forMemoryCardDeck(
            auditorId,  // operatorId
            deck.getCreatorId(),
            deck.getId(),
            previousState,
            deck.getPostId(),
            nodeId,
            postContentPreview,
            reason
        ));

        log.info("Deck {} restored by user {}", deckId, auditorId);
    }

    /**
     * 计算卡片组统计信息
     */
    /*
    private DeckStatsDTO calculateDeckStats(Long deckId) {
        if (deckId == null) {
            return new DeckStatsDTO();
        }
        
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDomainService.getById(deckId);
        if (deck == null) {
            return new DeckStatsDTO();
        }
        
        DeckStatsDTO stats = new DeckStatsDTO();
        stats.setTotalCards(deck.getCardCount());
        stats.setUpvotes(deck.getUpvoteCount());
        stats.setVersion(deck.getVersion());
        
        // TODO: 如需要更多统计信息，可以继续扩展
        // stats.setDownloads(获取下载数);
        // stats.setUsage(获取使用情况);
        
        return stats;
    }
     */

    // ========== 版本检测相关方法 ==========

    /**
     * 获取卡片组更新差异
     */
    public Map<String, Object> getDeckDiff(Long deckId, Integer userCurrentVersion, Long userId) {
        // 直接调用 DomainService
        return deckDomainService.getDeckDiff(deckId, userCurrentVersion, userId);
    }

    /**
     * 接受卡片组更新
     *
     * @param deckId 卡片组ID
     * @param cardIds 要接受的卡片ID列表（空表示接受所有）
     * @param courseId 当前浏览的课程ID（可选，用于创建 user_card_in_course 记录）
     * @param userId 用户ID
     */
    @Transactional
    public void acceptDeckChanges(Long deckId, List<Long> cardIds, Long courseId, Long userId) {
        // 验证卡片组存在
        MemoryCardDeckDO deck = deckDomainService.validateAndGet(deckId);

        // 获取nodeId：deck.sourcePostId → post.nodeId（跨域查询）
        Long nodeId = null;
        if (deck.getPostId() != null) {
            PostDO post = postDataService.getById(deck.getPostId());
            if (post != null) {
                nodeId = post.getNodeId();
            }
        }
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");

        // 调用 DomainService 执行接受更新
        deckDomainService.acceptDeckChanges(deckId, cardIds, userId, nodeId, courseId);
    }

    /**
     * 整体替换卡片组中的所有卡片
     */
    @Transactional
    public DeckFullDTO replaceAllCards(Long userId, Long deckId, CreateDeckRequest request) {
        // 删除现有所有卡片（调用 MemoryCardService）
        memoryCardService.deleteCardsByDeck(userId, deckId);

        // 调用 DomainService 执行替换
        MemoryCardDeckDO deck = deckDomainService.replaceAllCards(deckId, userId, request.getDescription());

        // 批量创建新卡片（调用 MemoryCardService）
        if (request.getCards() != null && !request.getCards().isEmpty()) {
            memoryCardService.batchCreateCards(userId, deckId, request.getCards());
            log.info("Replaced with {} new cards for deck {}", request.getCards().size(), deckId);
        }

        // 重新计算分数（跨域服务）
        scoreCalculationService.checkAndUpdateMemoryCardDeckScore(deck);
        // 需要再次更新 deck（因为分数被修改了）
        deckDomainService.updateDeck(deckId, userId, null);

        log.info("Replaced all cards for deck {} by user {}", deckId, userId);

        // 返回更新后的卡片组信息
        return deckAssembler.toFullDTO(deck, userId);
    }

    /**
     * AI生成记忆卡片组（异步任务）
     */
    @Transactional
    public void createAIDeck(Long userId, Long postId) {
        // 验证用户和帖子（跨域查询）
        userDataService.validateExists(userId);
        PostDO post = postDataService.validateAndGet(postId);

        // 只为文章类型的帖子生成记忆卡片
        if (!post.getType().equals(Enums.PostType.article.value())) {
            throw StatusCode.INVALID_PARAMETER.exception("只能为文章类型的帖子生成记忆卡片");
        }

        // 将任务加入队列（跨域服务）
        postQueueService.enqueueMemoryCards(postId);

        log.info("Queued AI memory card generation task for post {} requested by user {}", postId, userId);
    }

    /**
     * 删除卡片组（软删除）
     * @param deckId 卡片组ID
     * @param userId 用户ID
     */
    @Transactional
    public void deleteDeck(Long deckId, Long userId) {
        // 调用 DomainService 执行删除
        deckDomainService.deleteDeck(deckId, userId);
    }
}