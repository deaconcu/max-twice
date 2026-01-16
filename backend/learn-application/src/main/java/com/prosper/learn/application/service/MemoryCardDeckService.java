package com.prosper.learn.application.service;


import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.response.DeckDetailDTO;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.MemoryCardDeckDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.deck.DeckWithCreatorDTO;
import com.prosper.learn.application.dto.response.deck.DeckWithVoteDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.service.autoauthor.AutoAuthorQueueService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
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
    private final CourseDataService courseDataService;
    private final UpvoteService upvoteService;
    private final UserService userService;
    private final MemoryCardService memoryCardService;
    private final ScoreCalculationService scoreCalculationService;
    private final AutoAuthorQueueService autoAuthorQueueService;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

    // DTO转换器
    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;
    private final CourseConverter courseConverter;

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
     * 转换为卡片组（含创建者信息）
     */
    public DeckWithCreatorDTO toDeckWithCreator(MemoryCardDeckDO deckDO) {
        if (deckDO == null) return null;

        DeckWithCreatorDTO dto = deckConverter.toWithCreatorDTO(deckDO);

        // 填充创建者信息
        dto.setCreator(userService.toBriefDTO(userDataService.getById(deckDO.getCreatorId())));

        return dto;
    }

    /**
     * 转换为卡片组（含创建者信息和点赞状态）
     */
    public DeckWithVoteDTO toDeckWithVote(MemoryCardDeckDO deckDO, Long userId) {
        if (deckDO == null) return null;

        DeckWithVoteDTO dto = deckConverter.toWithVoteDTO(deckDO);

        // 填充创建者信息
        dto.setCreator(userService.toBriefDTO(userDataService.getById(deckDO.getCreatorId())));

        // 填充点赞状态（如果提供了用户ID）
        if (userId != null) {
            boolean hasUpvoted = upvoteService.getUpvoteStatus(deckDO.getId(), Enums.ContentType.memory_card_deck, userId).getLiked();
            dto.setHasLiked(hasUpvoted);
        }

        // 填充课程和节点信息
        if (deckDO.getNodeId() != null) {
            NodeDO nodeDO = nodeDataService.getById(deckDO.getNodeId());
            if (nodeDO != null) {
                // 设置节点信息
                NodeBriefDTO nodeDTO = new NodeBriefDTO();
                nodeDTO.setId(nodeDO.getId());
                nodeDTO.setName(nodeDO.getName());
                dto.setNode(nodeDTO);

                // 设置课程信息
                if (nodeDO.getCourseId() != null) {
                    dto.setCourseId(nodeDO.getCourseId());
                    CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
                    if (courseDO != null) {
                        dto.setCourse(courseConverter.toBriefDTO(courseDO));
                    }
                }
            }
        }

        return dto;
    }

    public List<DeckWithCreatorDTO> toDeckWithCreator(List<MemoryCardDeckDO> deckDOList) {
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
                DeckWithCreatorDTO dto = deckConverter.toWithCreatorDTO(deck);
                UserDO creator = userMap.get(deck.getCreatorId());
                if (creator != null) {
                    dto.setCreator(userConverter.toBriefDTO(creator));
                } else {
                    log.warn("Cannot find creator with id: {}", deck.getCreatorId());
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    public List<DeckWithVoteDTO> toDeckWithVote(List<MemoryCardDeckDO> deckDOList, Long userId) {
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
                boolean hasUpvoted = upvoteService.getUpvoteStatus(deckId, Enums.ContentType.memory_card_deck, userId).getLiked();
                upvoteStatusMap.put(deckId, hasUpvoted);
            }
        }

        // 批量获取课程和节点信息
        Map<Long, CourseBriefDTO> courseMap = new HashMap<>();
        Map<Long, NodeBriefDTO> nodeMap = new HashMap<>();
        Map<Long, Long> nodeToCourseMap = new HashMap<>(); // nodeId -> courseId的映射

        // 收集所有需要查询的nodeId
        Set<Long> nodeIds = deckDOList.stream()
            .map(MemoryCardDeckDO::getNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // 批量查询节点信息
        if (!nodeIds.isEmpty()) {
            Map<Long, NodeDO> nodeDOMap = nodeDataService.getMapByIds(nodeIds);

            // 收集courseId并建立nodeId到courseId的映射
            Set<Long> courseIds = new HashSet<>();
            for (NodeDO nodeDO : nodeDOMap.values()) {
                if (nodeDO.getCourseId() != null) {
                    courseIds.add(nodeDO.getCourseId());
                    nodeToCourseMap.put(nodeDO.getId(), nodeDO.getCourseId());
                }
            }

            // 批量查询课程信息
            if (!courseIds.isEmpty()) {
                Map<Long, CourseDO> courseDOMap = courseDataService.getMapByIds(courseIds);
                for (CourseDO courseDO : courseDOMap.values()) {
                    courseMap.put(courseDO.getId(), courseConverter.toBriefDTO(courseDO));
                }
            }

            // 转换节点信息
            for (NodeDO nodeDO : nodeDOMap.values()) {
                NodeBriefDTO nodeDTO = new NodeBriefDTO();
                nodeDTO.setId(nodeDO.getId());
                nodeDTO.setName(nodeDO.getName());
                nodeMap.put(nodeDO.getId(), nodeDTO);
            }
        }

        return deckDOList.stream()
            .map(deck -> {
                DeckWithVoteDTO dto = deckConverter.toWithVoteDTO(deck);
                UserDO creator = userMap.get(deck.getCreatorId());
                if (creator != null) {
                    dto.setCreator(userConverter.toBriefDTO(creator));
                } else {
                    log.warn("Cannot find creator with id: {}", deck.getCreatorId());
                }

                // 设置点赞状态
                if (userId != null) {
                    dto.setHasLiked(upvoteStatusMap.get(deck.getId()));
                }

                // 设置课程和节点信息
                if (deck.getNodeId() != null) {
                    // 设置节点信息
                    NodeBriefDTO node = nodeMap.get(deck.getNodeId());
                    if (node != null) {
                        dto.setNode(node);
                    }

                    // 设置课程信息
                    Long courseId = nodeToCourseMap.get(deck.getNodeId());
                    if (courseId != null) {
                        dto.setCourseId(courseId);
                        CourseBriefDTO course = courseMap.get(courseId);
                        if (course != null) {
                            dto.setCourse(course);
                        }
                    }
                }

                return dto;
            })
            .collect(Collectors.toList());
    }


    // ========= 业务方法(Query) ==========

    /**
     * 需求1: 获取帖子下的公共卡片组列表 - keyset分页，normal状态
     */
    public KeysetPageResponse<DeckWithVoteDTO> getPostPublicDecks(
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
    public KeysetPageResponse<DeckWithVoteDTO> getPostCreatorDeck(
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
    public KeysetPageResponse<DeckWithVoteDTO> getMyPostDeck(
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
    public KeysetPageResponse<DeckWithVoteDTO> getUserDecks(
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
    private KeysetPageResponse<DeckWithVoteDTO> buildDeckResponse(
            List<MemoryCardDeckDO> deckList, Integer limit, Long userId) {
        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为DTO (包含创建者信息和点赞状态)
        List<DeckWithVoteDTO> dtoList = toDeckWithVote(deckList, userId);

        // 构建响应
        KeysetPageResponse<DeckWithVoteDTO> response = new KeysetPageResponse<>();
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
    public KeysetPageResponse<DeckWithVoteDTO> getDecksByNode(
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
        List<DeckWithVoteDTO> dtoList = toDeckWithVote(deckList, userId);

        // 构建响应
        KeysetPageResponse<DeckWithVoteDTO> response = new KeysetPageResponse<>();
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
            // 按状态查询，管理员审核页面
            if (lastId != null) {
                deckList = deckDomainService.getListByStateWithIdPaging(defaultState, lastId, limit + 1);
            } else {
                deckList = deckDomainService.getListByStateForReview(defaultState, limit + 1);
            }
        }

        // 判断是否有更多数据
        boolean hasMore = deckList.size() > limit;
        if (hasMore) {
            deckList = deckList.subList(0, limit);
        }

        // 转换为基础DTO列表(包含创建者信息和点赞状态)
        List<DeckWithVoteDTO> baseDTOList = toDeckWithVote(deckList, userId);
        
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
                List<CardWithSrsDTO> cardViews = memoryCardService.toCardViewWithSrs(cards, null);
                deckCardsMap.put(deckId, cardViews);
            });
        }
        
        // 转换为DetailDTO并填充卡片信息
        List<DeckDetailDTO> dtoList = baseDTOList.stream()
            .map(baseDTO -> {
                // 转换为详情DTO
                DeckDetailDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
                
                // 从map中获取卡片列表
                List<CardWithSrsDTO> cards = deckCardsMap.getOrDefault(baseDTO.getId(), new ArrayList<>());
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
        MemoryCardDeckDO deck = deckDomainService.validateAndGet(deckId);

        // 先转换为基础DTO(包含创建者信息)
        DeckWithCreatorDTO baseDTO = toDeckWithCreator(deck);
        
        // 使用converter转换为详情DTO
        DeckDetailDTO detail = deckConverter.toDeckDetailDTO(baseDTO);
        
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
    public DeckWithCreatorDTO createDeck(Long userId, CreateDeckRequest request) {
        // 验证参数
        checkNotNull(request);

        // 验证用户存在性（跨域查询）
        userDataService.validateExists(userId);

        // 验证源帖子存在（跨域查询）
        Long nodeId = null;
        if (request.getSourcePostId() != null) {
            PostDO post = postDataService.validateAndGet(request.getSourcePostId());
            nodeId = post.getNodeId();
        }
        checkNotNull(nodeId, "无法获取卡片组关联的节点ID");

        // 调用 DomainService 创建卡片组
        MemoryCardDeckDO deck = deckDomainService.createDeck(
            userId,
            request.getSourcePostId(),
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
        return toDeckWithCreator(deck);
    }

    /**
     * 更新卡片组
     */
    @Transactional
    public void updateDeck(Long userId, Long deckId, String description) {
        // 调用 DomainService 更新卡片组（只更新描述，不更新标题）
        deckDomainService.updateDeck(
            deckId,
            userId,
            null,  // title 设为 null，不更新
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
            deck.getTitle(),
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
            deck.getTitle(),
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
            deck.getTitle(),
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
            deck.getTitle(),
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
            deck.getTitle(),
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
     */
    @Transactional
    public void acceptDeckChanges(Long deckId, List<Long> cardIds, Long userId) {
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
        deckDomainService.acceptDeckChanges(deckId, cardIds, userId, nodeId);
    }

    /**
     * 整体替换卡片组中的所有卡片
     */
    @Transactional
    public DeckWithVoteDTO replaceAllCards(Long userId, Long deckId, CreateDeckRequest request) {
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
        deckDomainService.updateDeck(deckId, userId, null, null);

        log.info("Replaced all cards for deck {} by user {}", deckId, userId);

        // 返回更新后的卡片组信息
        return toDeckWithVote(deck, userId);
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
        autoAuthorQueueService.enqueueMemoryCards(postId);

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

    // ========== 管理后台方法 ==========

    /**
     * 管理后台：按条件查询卡片组列表
     * @param state 状态（可选）
     * @param postId 帖子ID（可选）
     * @param creatorId 创建者ID（可选）
     * @param lastId 最后ID（分页）
     * @param limit 限制数量
     * @return 卡片组列表（分页响应）
     */
    public KeysetPageResponse<DeckWithCreatorDTO> getDecksForAdmin(Enums.ContentState state, Long postId, Long creatorId, Long lastId, Integer limit) {
        // 设置默认值
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100; // 最大100条
        }
        // lastId 保持 null，不设置默认值，由 Mapper 动态处理

        List<MemoryCardDeckDO> deckDOList;

        // 根据不同条件组合查询（查询 limit + 1 条用于判断是否有更多）
        int queryLimit = limit + 1;

        // 根据不同条件组合查询
        if (postId != null && creatorId != null) {
            // 按帖子和创建者查询
            if (state != null) {
                deckDOList = deckDomainService.getListByPostAndCreatorWithIdPaging(postId, creatorId, state, lastId, queryLimit);
            } else {
                deckDOList = deckDomainService.getListByPostAndCreatorAllStates(postId, creatorId, queryLimit);
            }
        } else if (postId != null) {
            // 按帖子查询
            if (state != null) {
                deckDOList = deckDomainService.getListByPostWithIdPaging(postId, state, lastId, queryLimit);
            } else {
                deckDOList = deckDomainService.getListByPost(postId, Enums.ContentState.SUBMITTED, queryLimit);
            }
        } else if (creatorId != null) {
            // 按创建者查询
            if (state != null) {
                deckDOList = deckDomainService.getListByCreatorWithIdPagingAndState(creatorId, state, lastId, queryLimit);
            } else {
                deckDOList = deckDomainService.getListByCreatorWithIdPaging(creatorId, lastId, queryLimit);
            }
        } else if (state != null) {
            // 只按状态查询
            deckDOList = deckDomainService.getListByStateWithIdPaging(state, lastId, queryLimit);
        } else {
            // 没有任何筛选条件，返回空列表
            throw StatusCode.INVALID_PARAMETER.exception("至少需要提供一个查询条件（state、postId 或 creatorId）");
        }

        // 判断是否有更多数据
        boolean hasMore = deckDOList.size() > limit;
        if (hasMore) {
            deckDOList = deckDOList.subList(0, limit);
        }

        // 转换为 DTO
        List<DeckWithCreatorDTO> dtoList = toDeckWithCreator(deckDOList);

        // 构建分页响应
        KeysetPageResponse<DeckWithCreatorDTO> response = new KeysetPageResponse<>();
        response.setItems(dtoList);
        response.setHasMore(hasMore);

        if (hasMore && !deckDOList.isEmpty()) {
            MemoryCardDeckDO lastDeck = deckDOList.get(deckDOList.size() - 1);
            KeysetPageResponse.NextCursor nextCursor = new KeysetPageResponse.NextCursor();
            nextCursor.setLastId(lastDeck.getId());
            response.setNextCursor(nextCursor);
        }

        return response;
    }
}