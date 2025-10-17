package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.ScoreCalculationService;
import com.prosper.learn.domain.service.data.*;
import com.prosper.learn.domain.service.autoauthor.AutoAuthorQueueService;
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
 * 记忆卡片组业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDeckService {

    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDataService memoryCardDataService;
    private final UserDataService userDataService;
    private final PostDataService postDataService;
    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;
    private final UserService userService;
    private final MemoryCardService memoryCardService;
    private final MemoryCardVersionDataService cardVersionDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final UpvoteService upvoteService;
    private final ScoreCalculationService scoreCalculationService;
    private final AutoAuthorQueueService autoAuthorQueueService;

    // ========== toDTO ==========

    /**
     * 基础DTO转换
     */
    public MemoryCardDeckDTO toDTO(MemoryCardDeckDO deckDO) {
        return deckConverter.toDTO(deckDO);
    }

    public List<MemoryCardDeckDTO> toDTO(List<MemoryCardDeckDO> deckDOList) {
        return deckConverter.toDTO(deckDOList);
    }

    /**
     * V1 = 基础信息 + 创建者信息
     */
    public MemoryCardDeckDTO toDTOV1(MemoryCardDeckDO deckDO) {
        if (deckDO == null) return null;

        MemoryCardDeckDTO dto = deckConverter.toDTO(deckDO);

        // 填充创建者信息
        UserDTO creator = userService.getUser(deckDO.getCreatorId(), DTOVersion.V2);
        dto.setCreator(creator);

        return dto;
    }

    /**
     * V1 = 基础信息 + 创建者信息 + 点赞状态
     */
    public MemoryCardDeckDTO toDTOV1(MemoryCardDeckDO deckDO, Long userId) {
        if (deckDO == null) return null;

        MemoryCardDeckDTO dto = deckConverter.toDTO(deckDO);

        // 填充创建者信息
        UserDTO creator = userService.getUser(deckDO.getCreatorId(), DTOVersion.V2);
        dto.setCreator(creator);

        // 填充点赞状态（如果提供了用户ID）
        if (userId != null) {
            boolean hasUpvoted = upvoteService.getUpvoteStatus(deckDO.getId(), ObjectType.memory_card_deck.value(), userId).getUpvoted();
            dto.setHasUpvoted(hasUpvoted);
        }

        return dto;
    }

    public List<MemoryCardDeckDTO> toDTOV1(List<MemoryCardDeckDO> deckDOList) {
        if (deckDOList == null || deckDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取创建者信息
        Set<Long> creatorIds = deckDOList.stream()
            .map(MemoryCardDeckDO::getCreatorId)
            .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = userDataService.getMapByIds(creatorIds);

        return deckDOList.stream()
            .map(deck -> {
                MemoryCardDeckDTO dto = deckConverter.toDTO(deck);
                UserDO creator = userMap.get(deck.getCreatorId());
                if (creator != null) {
                    dto.setCreator(userConverter.toDTO(creator));
                } else {
                    log.warn("Cannot find creator with id: {}", deck.getCreatorId());
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    public List<MemoryCardDeckDTO> toDTOV1(List<MemoryCardDeckDO> deckDOList, Long userId) {
        if (deckDOList == null || deckDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取创建者信息
        Set<Long> creatorIds = deckDOList.stream()
            .map(MemoryCardDeckDO::getCreatorId)
            .collect(Collectors.toSet());
        Map<Long, UserDO> userMap = userDataService.getMapByIds(creatorIds);

        // 批量获取点赞状态（如果提供了用户ID）
        Map<Long, Boolean> upvoteStatusMap = new HashMap<>();
        if (userId != null) {
            Set<Long> deckIds = deckDOList.stream()
                .map(MemoryCardDeckDO::getId)
                .collect(Collectors.toSet());
            // 批量查询点赞状态
            for (Long deckId : deckIds) {
                boolean hasUpvoted = upvoteService.getUpvoteStatus(deckId, ObjectType.memory_card_deck.value(), userId).getUpvoted();
                upvoteStatusMap.put(deckId, hasUpvoted);
            }
        }

        return deckDOList.stream()
            .map(deck -> {
                MemoryCardDeckDTO dto = deckConverter.toDTO(deck);
                UserDO creator = userMap.get(deck.getCreatorId());
                if (creator != null) {
                    dto.setCreator(userConverter.toDTO(creator));
                } else {
                    log.warn("Cannot find creator with id: {}", deck.getCreatorId());
                }

                // 设置点赞状态
                if (userId != null) {
                    dto.setHasUpvoted(upvoteStatusMap.get(deck.getId()));
                }

                return dto;
            })
            .collect(Collectors.toList());
    }


    // ========= 业务方法(Query) ==========

    /**
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
     */
    public KeysetPageResponse<MemoryCardDeckDTO> getPostPublicDecks(
            Long postId, String sortBy, String sortOrder, Double lastScore, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;
        if (lastScore != null && lastId != null) {
            deckList = deckDataService.getListByPostKeyset(postId, lastScore, lastId, MemoryCardDeckState.NORMAL.value(), limit + 1);
        } else {
            deckList = deckDataService.getListByPost(postId, MemoryCardDeckState.NORMAL.value(), limit + 1);
        }

        return buildDeckResponse(deckList, limit, null);
    }

    /**
     * 需求2: 获取帖子创建者提交的卡片组 - 最新创建，limit通常为1
     */
    public KeysetPageResponse<MemoryCardDeckDTO> getPostCreatorDeck(
            Long postId, String sortBy, String sortOrder, Double lastScore, Long lastId, Integer limit, Long userId) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 1;
        if (limit > 50) limit = 50;

        PostDO post = postDataService.validateAndGet(postId);
        Long postCreatorId = post.getCreatorId();

        List<MemoryCardDeckDO> deckList;
        if (!postCreatorId.equals(userId)) {
            // post创建者不是当前用户，只查询normal状态
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByPostAndCreatorKeyset(
                        postId, postCreatorId, lastScore, lastId, MemoryCardDeckState.NORMAL.value(), limit + 1);
            } else {
                deckList = deckDataService.getListByPostAndCreator(
                        postId, postCreatorId, MemoryCardDeckState.NORMAL.value(), limit + 1);
            }
        } else {
            // post创建者就是当前用户，查询所有状态
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByPostAndCreatorKeysetAllStates(
                        postId, postCreatorId, lastScore, lastId, limit + 1);
            } else {
                deckList = deckDataService.getListByPostAndCreatorAllStates(
                        postId, postCreatorId, limit + 1);
            }
        }

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 需求3: 获取用户自己在指定帖子下提交的卡片组 - 最新创建，limit通常为1
     */
    public KeysetPageResponse<MemoryCardDeckDTO> getMyPostDeck(
            Long postId, Long userId, String sortBy, String sortOrder, Double lastScore, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 1;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;
        if (lastScore != null && lastId != null) {
            deckList = deckDataService.getListByPostAndCreatorKeysetAllStates(postId, userId, lastScore, lastId, limit + 1);
        } else {
            deckList = deckDataService.getListByPostAndCreatorAllStates(postId, userId, limit + 1);
        }

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 需求4: 获取用户自己提交的所有卡片组 - keyset分页，全部状态
     */
    public KeysetPageResponse<MemoryCardDeckDTO> getMyAllDecks(
            Long userId, String sortBy, String sortOrder, Double lastScore, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;
        // 暂时查询正常状态，TODO: 需要添加查询所有状态的方法
        if (lastScore != null && lastId != null) {
            deckList = deckDataService.getListByCreatorKeyset(
                    userId, lastScore, lastId, MemoryCardDeckState.NORMAL.value(), limit + 1);
        } else {
            deckList = deckDataService.getListByCreator(userId, MemoryCardDeckState.NORMAL.value(), limit + 1);
        }

        return buildDeckResponse(deckList, limit, userId);
    }

    /**
     * 构建卡片组响应的通用方法
     */
    private KeysetPageResponse<MemoryCardDeckDTO> buildDeckResponse(
            List<MemoryCardDeckDO> deckList, Integer limit, Long userId) {
        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息和点赞状态)
        List<MemoryCardDeckDTO> dtoList = toDTOV1(deckList, userId);

        // 构建响应
        KeysetPageResponse<MemoryCardDeckDTO> response = new KeysetPageResponse<>();
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
    public KeysetPageResponse<MemoryCardDeckDTO> getDecksByNode(
            Long nodeId, Double lastScore, Long lastId, Integer limit, Long userId) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 20;
        if (limit > 50) limit = 50;

        List<MemoryCardDeckDO> deckList;

        // 只查询正常状态的卡片组
        int state = MemoryCardDeckState.NORMAL.value();

        if (lastScore != null && lastId != null) {
            deckList = deckDataService.getListByNodeKeyset(nodeId, lastScore, lastId, state, limit + 1);
        } else {
            deckList = deckDataService.getListByNode(nodeId, state, limit + 1);
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息和点赞状态)
        List<MemoryCardDeckDTO> dtoList = toDTOV1(deckList, userId);

        // 构建响应
        KeysetPageResponse<MemoryCardDeckDTO> response = new KeysetPageResponse<>();
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
     * 获取卡片组审核列表 - 包含卡片内容
     */
    public KeysetPageResponse<DeckDetailDTO> getDecksForReview(
            Long postId, Long creatorId, Integer state, Long lastId, Long userId) {

        // 固定每页数量
        int limit = 20;

        // 默认查询待审核状态的卡片组
        int defaultState = state != null ? state : MemoryCardDeckState.PENDING.value();

        List<MemoryCardDeckDO> deckList;

        // 根据查询条件组合获取数据 - 统一使用ID分页
        if (postId != null && creatorId != null) {
            // 同时按帖子和创建者查询
            if (lastId != null) {
                deckList = deckDataService.getListByPostAndCreatorWithIdPaging(postId, creatorId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDataService.getListByPostAndCreatorForReview(postId, creatorId, defaultState, limit + 1);
            }
        } else if (postId != null) {
            // 只按帖子查询
            if (lastId != null) {
                deckList = deckDataService.getListByPostWithIdPaging(postId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDataService.getListByPostForReview(postId, defaultState, limit + 1);
            }
        } else if (creatorId != null) {
            // 只按创建者查询
            if (lastId != null) {
                deckList = deckDataService.getListByCreatorWithIdPaging(creatorId, defaultState, lastId, limit + 1);
            } else {
                deckList = deckDataService.getListByCreatorForReview(creatorId, defaultState, limit + 1);
            }
        } else {
            // 按状态查询，管理员审核页面
            if (lastId != null) {
                deckList = deckDataService.getListByStateWithIdPaging(defaultState, lastId, limit + 1);
            } else {
                deckList = deckDataService.getListByStateForReview(defaultState, limit + 1);
            }
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为基础DTO列表(包含创建者信息和点赞状态)
        List<MemoryCardDeckDTO> baseDTOList = toDTOV1(deckList, userId);
        
        // 批量获取所有卡片组的卡片信息
        Set<Long> deckIds = deckList.stream().map(MemoryCardDeckDO::getId).collect(Collectors.toSet());
        Map<Long, List<MemoryCardViewDTO>> deckCardsMap = new HashMap<>();
        
        if (!deckIds.isEmpty()) {
            // 直接从数据层批量查询卡片
            List<MemoryCardDO> allCards = memoryCardDataService.getByDeckIds(new ArrayList<>(deckIds));
            
            // 按deckId分组
            Map<Long, List<MemoryCardDO>> cardsByDeck = allCards.stream()
                .collect(Collectors.groupingBy(MemoryCardDO::getDeckId));
            
            // 转换为DTO
            cardsByDeck.forEach((deckId, cards) -> {
                List<MemoryCardViewDTO> cardViews = memoryCardService.toDTOV1(cards, null);
                deckCardsMap.put(deckId, cardViews);
            });
        }
        
        // 转换为DetailDTO并填充卡片信息
        List<DeckDetailDTO> dtoList = baseDTOList.stream()
            .map(baseDTO -> {
                // 转换为详情DTO
                DeckDetailDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
                
                // 从map中获取卡片列表
                List<MemoryCardViewDTO> cards = deckCardsMap.getOrDefault(baseDTO.getId(), new ArrayList<>());
                detail.setCards(cards);
                
                return detail;
            })
            .collect(Collectors.toList());

        // 构建响应
        KeysetPageResponse<DeckDetailDTO> response = new KeysetPageResponse<>();
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
    public DeckDetailDTO getDeckDetail(Long deckId, Long userId) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 先转换为基础DTO(包含创建者信息)
        MemoryCardDeckDTO baseDTO = toDTOV1(deck);
        
        // 使用converter转换为详情DTO
        DeckDetailDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
        
        // 获取卡片列表（所有卡片tab - 不包含用户学习状态，显示最新版本）
        List<MemoryCardViewDTO> cards = memoryCardService.getCardsByDeck(deckId, null);
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
    public MemoryCardDeckDTO createDeck(Long userId, CreateDeckRequest request) {
        // 验证参数
        checkNotNull(request);

        // 验证用户存在性
        userDataService.validateExists(userId);

        // 验证源帖子存在（如果指定了）
        Long nodeId = null;
        if (request.getSourcePostId() != null) {
            PostDO post = postDataService.validateAndGet(request.getSourcePostId());
            nodeId = post.getNodeId();
        }
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");

        // 构建卡片组DO
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setSourcePostId(request.getSourcePostId());
        deck.setNodeId(nodeId);
        deck.setCreatorId(userId);
        deck.setTitle(request.getTitle());
        deck.setDescription(request.getDescription());
        deck.setVersion(1);
        deck.setState(MemoryCardDeckState.PENDING.value()); // 默认审核中
        deck.setCardCount(request.getCards() != null ? request.getCards().size() : 0);

        // 插入数据库
        int result = deckDataService.insert(deck);
        if (result <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("创建卡片组失败");
        }

        // 创建完成后立即计算初始分数
        // TODO: 先注释掉，新deck需要有一个启动分数，后续再调整分数计算逻辑
        //scoreCalculationService.checkAndUpdateMemoryCardDeckScore(deck);
        //deckDataService.update(deck);

        // 如果有卡片数据，批量创建卡片
        if (request.getCards() != null && !request.getCards().isEmpty()) {
            memoryCardService.batchCreateCards(userId, deck.getId(), request.getCards());
            log.info("Created deck {} with {} cards", deck.getId(), request.getCards().size());
        }

        // 转换并返回 (包含创建者信息)
        return toDTOV1(deck);
    }

    /**
     * 更新卡片组
     */
    @Transactional
    public MemoryCardDeckDTO updateDeck(Long userId, UpdateDeckRequest request) {
        // 验证参数
        checkNotNull(request);

        // 获取现有卡片组
        MemoryCardDeckDO existingDeck = deckDataService.validateAndGet(request.getId());

        // 验证权限：只有创建者可以修改
        if (!existingDeck.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限修改此卡片组");
        }

        // 更新字段
        MemoryCardDeckDO deck = deckDataService.getById(request.getId());
        if (request.getTitle() != null) {
            deck.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            deck.setDescription(request.getDescription());
        }
        //deck.setUpdatedBy(userId);
        deck.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        deckDataService.update(deck);

        // 获取更新后的数据并返回 (包含创建者信息)
        return toDTOV1(deck);
    }

    /**
     * 审核通过卡片组
     */
    @Transactional
    public void approveDeck(Long deckId, Long auditorId) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);
        
        // 验证状态：只有待审核的卡片组才能通过
        if (deck.getState() != MemoryCardDeckState.PENDING.value()) {
            throw ErrorCode.INVALID_PARAMETER.exception("只有待审核状态的卡片组才能通过审核");
        }
        
        // 更新状态为正常
        deck.setState(MemoryCardDeckState.NORMAL.value());
        //deck.setUpdatedBy(auditorId);
        deck.setUpdatedAt(LocalDateTime.now());
        
        deckDataService.update(deck);
        log.info("Deck {} approved by user {}", deckId, auditorId);
    }

    /**
     * 废弃卡片组（合并原 rejectDeck 和 blockDeck 功能）
     */
    @Transactional
    public void discardDeck(Long deckId, Long auditorId) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证状态：只有待审核或正常状态的卡片组才能废弃
        if (deck.getState() != MemoryCardDeckState.PENDING.value() &&
            deck.getState() != MemoryCardDeckState.NORMAL.value()) {
            throw ErrorCode.INVALID_PARAMETER.exception("只有待审核或正常状态的卡片组才能废弃");
        }

        // 更新状态为屏蔽
        deck.setState(MemoryCardDeckState.BLOCKED.value());
        //deck.setUpdatedBy(auditorId);
        deck.setUpdatedAt(LocalDateTime.now());

        deckDataService.update(deck);
        log.info("Deck {} discarded by user {}", deckId, auditorId);
    }

    /**
     * 恢复卡片组
     */
    @Transactional
    public void restoreDeck(Long deckId, Long auditorId) {
        // 获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);
        
        // 验证状态：只有屏蔽状态的卡片组才能恢复
        if (deck.getState() != MemoryCardDeckState.BLOCKED.value()) {
            throw ErrorCode.INVALID_PARAMETER.exception("只有屏蔽状态的卡片组才能恢复");
        }
        
        // 更新状态为正常
        deck.setState(MemoryCardDeckState.NORMAL.value());
        //deck.setUpdatedBy(auditorId);
        deck.setUpdatedAt(LocalDateTime.now());
        
        deckDataService.update(deck);
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
        MemoryCardDeckDO deck = deckDataService.getById(deckId);
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
        diffResult.put("title", currentDeck.getTitle());
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
     */
    @Transactional
    public void acceptDeckChanges(Long deckId, List<Long> cardIds, Long userId) {
        // 验证卡片组存在
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);
        
        // 获取nodeId：deck.sourcePostId → post.nodeId
        Long nodeId = null;
        if (deck.getSourcePostId() != null) {
            PostDO post = postDataService.getById(deck.getSourcePostId());
            if (post != null) {
                nodeId = post.getNodeId();
            }
        }
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");
        
        // 获取用户在这个deck下的所有学习记录
        List<UserCardSrsDO> userStates = userCardSrsDataService.getByUserAndDeckId(userId, deckId);
        Map<Long, UserCardSrsDO> userStateMap = userStates.stream()
            .collect(Collectors.toMap(UserCardSrsDO::getCardId, state -> state));
        
        // 获取当前deck中的所有有效卡片
        List<MemoryCardDO> currentCards = memoryCardDataService.getByDeckId(deckId);
        Map<Long, MemoryCardDO> currentCardMap = currentCards.stream()
            .collect(Collectors.toMap(MemoryCardDO::getId, card -> card));
        
        // 准备新增卡片的SRS状态列表
        List<UserCardSrsDO> newSrsStates = new ArrayList<>();
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
                    log.info("Deleted SRS state for removed card: {} for user: {}", userState.getCardId(), userId);
                }
            }
            
            // 2. 处理新增卡片
            for (MemoryCardDO card : currentCards) {
                if (!userStateMap.containsKey(card.getId())) {
                    // 新增的卡片：创建SRS学习记录
                    UserCardSrsDO newState = userCardSrsDataService.createNewSrsState(
                        userId, card.getId(), nodeId, deck.getId(), deck.getVersion(), card.getCurrentVersionId());
                    newSrsStates.add(newState);
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
                        log.info("Deleted SRS state for removed card: {} for user: {}", userState.getCardId(), userId);
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
                            userId, card.getId(), nodeId, deck.getId(), deck.getVersion(), card.getCurrentVersionId());
                        newSrsStates.add(newState);
                    }
                }
            }
        }
        
        // 批量插入新增卡片的SRS状态
        if (!newSrsStates.isEmpty()) {
            userCardSrsDataService.batchInsertIgnoreSrsStates(newSrsStates);
            log.info("Created {} new SRS states for added cards for user: {}", newSrsStates.size(), userId);
        }
        
        log.info("User {} accepted changes for deck {} with {} target items",
                userId, deckId, cardIds.isEmpty() ? userStates.size() + newSrsStates.size() : cardIds.size());
    }

    /**
     * 整体替换卡片组中的所有卡片
     */
    @Transactional
    public MemoryCardDeckDTO replaceAllCards(Long userId, Long deckId, CreateDeckRequest request) {
        // 验证并获取卡片组
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 验证权限：只有创建者可以替换卡片
        if (!deck.getCreatorId().equals(userId)) {
            throw ErrorCode.PERMISSION_DENIED.exception("无权限修改此卡片组");
        }

        // 删除现有所有卡片
        memoryCardService.deleteCardsByDeck(userId, deckId);

        // 更新卡片组信息（如果提供了）
        if (request.getTitle() != null) {
            deck.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            deck.setDescription(request.getDescription());
        }

        // 设置为待审核状态
        deck.setState(MemoryCardDeckState.PENDING.value());
        deck.setUpdatedAt(LocalDateTime.now());
        //deck.setUpdatedBy(userId);
        deckDataService.update(deck);

        // 批量创建新卡片
        if (request.getCards() != null && !request.getCards().isEmpty()) {
            memoryCardService.batchCreateCards(userId, deckId, request.getCards());
            log.info("Replaced with {} new cards for deck {}", request.getCards().size(), deckId);
        }

        // 重新计算分数
        scoreCalculationService.checkAndUpdateMemoryCardDeckScore(deck);
        deckDataService.update(deck);

        log.info("Replaced all cards for deck {} by user {}", deckId, userId);

        // 返回更新后的卡片组信息
        return toDTOV1(deck, userId);
    }

    /**
     * AI生成记忆卡片组（异步任务）
     */
    @Transactional
    public void createAIDeck(Long userId, Long postId) {
        // 验证用户和帖子
        userDataService.validateExists(userId);
        PostDO post = postDataService.validateAndGet(postId);

        // 只为文章类型的帖子生成记忆卡片
        if (!post.getType().equals(PostType.article.value())) {
            throw ErrorCode.INVALID_PARAMETER.exception("只能为文章类型的帖子生成记忆卡片");
        }

        // 将任务加入队列
        autoAuthorQueueService.enqueueMemoryCards(postId);

        log.info("Queued AI memory card generation task for post {} requested by user {}", postId, userId);
    }
}