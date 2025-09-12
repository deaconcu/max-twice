package com.prosper.learn.domain.service.business;

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
 * 记忆卡片组业务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCardDeckService {

    private final MemoryCardDeckDataService deckDataService;
    private final UserDataService userDataService;
    private final PostDataService postDataService;
    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;
    private final UserService userService;
    private final MemoryCardService memoryCardService;

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
                }
                return dto;
            })
            .collect(Collectors.toList());
    }


    /**
     * 获取卡片组列表 - Keyset分页
     * 在read页面的右边栏显示卡片组列表
     */
    public KeysetPageResponse<MemoryCardDeckDTO> getDeckList(
            Long postId, Long creatorId, Integer state, String sortBy, String sortOrder,
            Double lastScore, Long lastId, Integer limit) {
        // 参数验证
        if (limit == null || limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        // 默认查询正常状态的卡片组
        int defaultState = state != null ? state : MemoryCardDeckState.NORMAL.value();

        List<MemoryCardDeckDO> deckList;
        
        // 根据查询条件组合获取数据
        if (postId != null && creatorId != null) {
            // 同时按帖子和创建者查询，read页右边栏，只看帖子的创建者的卡片组
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByPostAndCreatorKeyset(postId, creatorId, lastScore, lastId, defaultState, limit + 1);
            } else {
                deckList = deckDataService.getListByPostAndCreator(postId, creatorId, defaultState, limit + 1);
            }
        } else if (postId != null) {
            // 只按帖子查询，read页右边栏，显示帖子的所有卡片组
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByPostKeyset(postId, lastScore, lastId, defaultState, limit + 1);
            } else {
                deckList = deckDataService.getListByPost(postId, defaultState, limit + 1);
            }
        } else if (creatorId != null) {
            // 只按创建者查询，用户主页，显示用户创建的所有卡片组
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByCreatorKeyset(creatorId, lastScore, lastId, defaultState, limit + 1);
            } else {
                deckList = deckDataService.getListByCreator(creatorId, defaultState, limit + 1);
            }
        } else {
            // 按状态查询, 管理员后台，按状态显示所有卡片组
            if (lastScore != null && lastId != null) {
                deckList = deckDataService.getListByStateKeyset(lastScore, lastId, defaultState, limit + 1);
            } else {
                deckList = deckDataService.getListByState(defaultState, limit + 1);
            }
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息)
        List<MemoryCardDeckDTO> dtoList = toDTOV1(deckList);

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
     * 获取卡片组详情
     */
    public DeckDetailDTO getDeckDetail(Long deckId, Long userId) {
        // 获取卡片组信息
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 先转换为基础DTO(包含创建者信息)
        MemoryCardDeckDTO baseDTO = toDTOV1(deck);
        
        // 使用converter转换为详情DTO
        DeckDetailDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
        
        // 获取卡片列表
        List<MemoryCardViewDTO> cards = memoryCardService.getCardsByDeck(deckId, userId);
        detail.setCards(cards);
        
        // 获取统计信息
        //DeckStatsDTO stats = calculateDeckStats(deckId);
        //detail.setStats(stats);
        
        return detail;
    }

    // ========== 业务方法 ==========

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
        if (request.getSourcePostId() != null) {
            postDataService.validateExists(request.getSourcePostId());
        }

        // 构建卡片组DO
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setSourcePostId(request.getSourcePostId());
        deck.setCreatorId(userId);
        deck.setTitle(request.getTitle());
        deck.setDescription(request.getDescription());
        deck.setVersion(1);
        deck.setState(MemoryCardDeckState.PENDING.value()); // 默认审核中
        deck.setUpvoteCount(0);
        deck.setCardCount(0);
        deck.setScore(0.0);
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());

        // 插入数据库
        int result = deckDataService.insert(deck);
        if (result <= 0) {
            throw ErrorCode.SYSTEM_ERROR.exception("创建卡片组失败");
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
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setId(request.getId());
        if (request.getTitle() != null) {
            deck.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            deck.setDescription(request.getDescription());
        }
        deck.setUpdatedBy(userId);
        deck.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        deckDataService.update(deck);

        // 获取更新后的数据并返回 (包含创建者信息)
        MemoryCardDeckDO updatedDeck = deckDataService.getById(request.getId());
        return toDTOV1(updatedDeck);
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
}