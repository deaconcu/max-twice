package com.prosper.learn.application.assembler;

import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.UserCardSrsConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.UserCardSrsDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.deck.DeckBriefDTO;
import com.prosper.learn.application.service.UserService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardVersionDO;
import com.prosper.learn.memory.card.MemoryCardVersionDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Card DTO 组装器
 * 负责将 MemoryCardDO 转换为各种 DTO（需要联查数据库）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardAssembler {

    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;
    private final UserCardSrsConverter srsStateConverter;

    private final MemoryCardVersionDataService cardVersionDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final UserDataService userDataService;
    private final UserCardSrsDataService userCardSrsDataService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;

    private final UserService userService;

    // ========== DeckBriefDTO ==========

    /**
     * 转换为 DeckBriefDTO（含 courseId, nodeName, courseName）
     */
    public DeckBriefDTO toDeckBriefDTO(MemoryCardDeckDO deck) {
        if (deck == null) return null;

        DeckBriefDTO dto = deckConverter.toBriefDTO(deck);
        if (deck.getNodeId() != null) {
            NodeDO node = nodeDataService.getById(deck.getNodeId());
            if (node != null) {
                dto.setNodeName(node.getName());
                if (node.getCourseId() != null) {
                    dto.setCourseId(node.getCourseId());
                    CourseDO course = courseDataService.getById(node.getCourseId());
                    if (course != null) {
                        dto.setCourseName(course.getName());
                    }
                }
            }
        }
        return dto;
    }

    /**
     * 批量转换为 DeckBriefDTO
     */
    public Map<Long, DeckBriefDTO> toDeckBriefDTOMap(Map<Long, MemoryCardDeckDO> deckMap) {
        if (deckMap == null || deckMap.isEmpty()) {
            return new HashMap<>();
        }

        // 收集所有 nodeId
        Set<Long> nodeIds = deckMap.values().stream()
                .map(MemoryCardDeckDO::getNodeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询节点
        Map<Long, NodeDO> nodeMap = nodeIds.isEmpty() ? Map.of() : nodeDataService.getMapByIds(nodeIds);

        // 收集所有 courseId
        Set<Long> courseIds = nodeMap.values().stream()
                .map(NodeDO::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询课程
        Map<Long, CourseDO> courseMap = courseIds.isEmpty() ? Map.of() : courseDataService.getMapByIds(courseIds);

        // 转换
        Map<Long, DeckBriefDTO> result = new HashMap<>();
        for (Map.Entry<Long, MemoryCardDeckDO> entry : deckMap.entrySet()) {
            MemoryCardDeckDO deck = entry.getValue();
            DeckBriefDTO dto = deckConverter.toBriefDTO(deck);

            if (deck.getNodeId() != null) {
                NodeDO node = nodeMap.get(deck.getNodeId());
                if (node != null) {
                    dto.setNodeName(node.getName());
                    if (node.getCourseId() != null) {
                        dto.setCourseId(node.getCourseId());
                        CourseDO course = courseMap.get(node.getCourseId());
                        if (course != null) {
                            dto.setCourseName(course.getName());
                        }
                    }
                }
            }
            result.put(entry.getKey(), dto);
        }
        return result;
    }

    // ========== CardWithSrsDTO (单个) ==========

    /**
     * 转换为卡片（含SRS状态）
     */
    public CardWithSrsDTO toCardWithSrs(MemoryCardDO cardDO, Long userId) {
        if (cardDO == null) return null;

        CardWithSrsDTO dto = new CardWithSrsDTO();
        dto.setId(cardDO.getId());
        dto.setState(cardDO.getState());

        // 填充创建者信息
        dto.setCreator(userService.toBriefDTO(userDataService.getById(cardDO.getCreatorId())));

        MemoryCardDeckDO deck = deckDataService.validateAndGet(cardDO.getDeckId());
        dto.setDeck(toDeckBriefDTO(deck));

        // 获取SRS状态并检测更新
        UserCardSrsDO srsState = null;
        if (userId != null) {
            srsState = userCardSrsDataService.getByUserAndCard(userId, cardDO.getId());
            if (srsState != null) {
                UserCardSrsDTO srsDTO = srsStateConverter.toDTO(srsState);
                // 设置课程信息
                if (srsState.getCourseId() != null) {
                    CourseDO course = courseDataService.getById(srsState.getCourseId());
                    if (course != null) {
                        CourseBriefDTO courseBriefDTO = new CourseBriefDTO();
                        courseBriefDTO.setId(course.getId());
                        courseBriefDTO.setName(course.getName());
                        courseBriefDTO.setIcon(course.getIcon());
                        srsDTO.setCourse(courseBriefDTO);
                    }
                }
                dto.setSrsState(srsDTO);

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

        // 检查卡片或卡片组是否被屏蔽
        boolean isBlocked = isCardOrDeckBlocked(cardDO, deck, userId);

        // 获取卡片内容版本
        if (isBlocked) {
            // 被屏蔽的卡片不返回实际内容
            dto.setFront("blocked");
            dto.setBack("blocked");
        } else {
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
        }

        return dto;
    }

    /**
     * 检查卡片或卡片组是否被屏蔽
     * 如果 userId 是卡片或卡片组的创建者，则不屏蔽
     * 如果用户是审核员及以上角色，则不屏蔽
     */
    private boolean isCardOrDeckBlocked(MemoryCardDO card, MemoryCardDeckDO deck, Long userId) {
        if (userId != null) {
            // 创建者始终可以看到自己的内容
            if (userId.equals(card.getCreatorId())) {
                return false;
            }
            if (deck != null && userId.equals(deck.getCreatorId())) {
                return false;
            }
            // 审核员及以上角色可以看到被屏蔽的内容
            UserDO user = userDataService.getById(userId);
            if (user != null && user.hasRole(Enums.UserRole.MODERATOR)) {
                return false;
            }
        }
        // 卡片状态不是 PUBLISHED
        if (card.getState() != null && card.getState() != Enums.ContentState.PUBLISHED_VALUE) {
            return true;
        }
        // 卡片组状态不是 PUBLISHED
        if (deck != null && deck.getState() != null && deck.getState() != Enums.ContentState.PUBLISHED_VALUE) {
            return true;
        }
        return false;
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

    // ========== CardWithSrsDTO (批量) ==========

    /**
     * 批量转换（带预先获取的SRS状态）
     */
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

        // 批量转换 DeckBriefDTO
        Map<Long, DeckBriefDTO> deckBriefMap = toDeckBriefDTOMap(deckMap);

        // 批量获取课程信息（用于 SRS 状态中的课程）
        Set<Long> courseIds = srsStateMap.values().stream()
                .map(UserCardSrsDO::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CourseDO> courseMap = courseIds.isEmpty() ? Map.of() : courseDataService.getMapByIds(courseIds);

        return cardDOList.stream()
                .map(card -> {
                    CardWithSrsDTO dto = new CardWithSrsDTO();
                    dto.setId(card.getId());
                    dto.setState(card.getState());

                    // 设置创建者信息
                    UserDO creator = userMap.get(card.getCreatorId());
                    if (creator != null) {
                        dto.setCreator(userConverter.toBriefDTO(creator));
                    }

                    // 获取SRS状态并检测更新
                    UserCardSrsDO srsState = srsStateMap.get(card.getId());
                    if (srsState != null) {
                        UserCardSrsDTO srsDTO = srsStateConverter.toDTO(srsState);
                        // 设置课程信息
                        if (srsState.getCourseId() != null) {
                            CourseDO course = courseMap.get(srsState.getCourseId());
                            if (course != null) {
                                CourseBriefDTO courseBriefDTO = new CourseBriefDTO();
                                courseBriefDTO.setId(course.getId());
                                courseBriefDTO.setName(course.getName());
                                courseBriefDTO.setIcon(course.getIcon());
                                srsDTO.setCourse(courseBriefDTO);
                            }
                        }
                        dto.setSrsState(srsDTO);

                        // 检测deck是否有更新
                        MemoryCardDeckDO deckForUpdate = deckMap.get(card.getDeckId());
                        if (deckForUpdate != null && srsState.getDeckVersion() != null) {
                            boolean hasDeckUpdate = !srsState.getDeckVersion().equals(deckForUpdate.getVersion());
                            dto.setHasDeckUpdate(hasDeckUpdate);
                        }

                        // 检测卡片内容是否有更新
                        if (srsState.getCardVersionId() != null) {
                            boolean hasCardUpdate = !srsState.getCardVersionId().equals(card.getCurrentVersionId());
                            dto.setHasCardUpdate(hasCardUpdate);
                        }
                    }

                    // 检查卡片或卡片组是否被屏蔽
                    MemoryCardDeckDO deck = deckMap.get(card.getDeckId());
                    boolean isBlocked = isCardOrDeckBlocked(card, deck, userId);

                    // 设置卡片内容 - 根据场景选择版本
                    if (isBlocked) {
                        // 被屏蔽的卡片不返回实际内容
                        dto.setFront("blocked");
                        dto.setBack("blocked");
                    } else {
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
                    }

                    if (deckBriefMap.containsKey(card.getDeckId())) {
                        dto.setDeck(deckBriefMap.get(card.getDeckId()));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 批量转换（自动获取SRS状态）
     */
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
}
