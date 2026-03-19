package com.prosper.learn.application.assembler;

import com.prosper.learn.analytics.dto.ContentStatsDTO;
import com.prosper.learn.analytics.stats.service.ContentStatsDomainService;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.deck.DeckFullDTO;
import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.service.BookmarkService;
import com.prosper.learn.application.service.UpvoteService;
import com.prosper.learn.application.service.UserService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
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
 * Deck DTO 组装器
 * 负责将 MemoryCardDeckDO 转换为各种 DTO（需要联查数据库）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeckAssembler {

    private final MemoryCardDeckConverter deckConverter;
    private final UserConverter userConverter;
    private final CourseConverter courseConverter;

    private final UserDataService userDataService;
    private final NodeDataService nodeDataService;
    private final CourseDataService courseDataService;
    private final ContentStatsDomainService contentStatsDomainService;

    private final UpvoteService upvoteService;
    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final UserCardSrsDataService userCardSrsDataService;

    // ========== Full DTO (单个) ==========

    /**
     * 转换为卡片组（含创建者信息）
     */
    public DeckFullDTO toFullDTO(MemoryCardDeckDO deckDO) {
        if (deckDO == null) return null;

        DeckFullDTO dto = deckConverter.toFullDTO(deckDO);
        dto.setCreator(userService.toBriefDTO(userDataService.getById(deckDO.getCreatorId())));
        return dto;
    }

    /**
     * 转换为卡片组（含创建者信息和点赞状态）
     */
    public DeckFullDTO toFullDTO(MemoryCardDeckDO deckDO, Long userId) {
        if (deckDO == null) return null;

        DeckFullDTO dto = deckConverter.toFullDTO(deckDO);

        // 填充创建者信息
        dto.setCreator(userService.toBriefDTO(userDataService.getById(deckDO.getCreatorId())));

        // 填充点赞状态（如果提供了用户ID）
        if (userId != null) {
            boolean hasUpvoted = upvoteService.getUpvoteStatus(deckDO.getId(), Enums.ContentType.memory_card_deck, userId).getLiked();
            dto.setHasLiked(hasUpvoted);
            dto.setBookmarked(bookmarkService.isBookmarked(userId, deckDO.getId(), Enums.ContentType.memory_card));
        } else {
            dto.setBookmarked(false);
        }

        // 填充统计数据（包含 Redis 实时增量）
        ContentStatsDTO stats = contentStatsDomainService.getContentStats(Enums.ContentType.memory_card_deck, deckDO.getId());
        if (stats != null) {
            dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
        }

        // 填充课程和节点信息
        fillNodeAndCourse(dto, deckDO.getNodeId());

        return dto;
    }

    // ========== Full DTO (批量) ==========

    /**
     * 批量转换为卡片组（含创建者信息）
     */
    public List<DeckFullDTO> toFullDTO(List<MemoryCardDeckDO> deckDOList) {
        if (deckDOList == null || deckDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量获取创建者信息
        Map<Long, UserDO> userMap = batchGetCreators(deckDOList);

        return deckDOList.stream()
            .map(deck -> {
                DeckFullDTO dto = deckConverter.toFullDTO(deck);
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

    /**
     * 批量转换为卡片组（含创建者信息、点赞状态、课程节点信息）
     */
    public List<DeckFullDTO> toFullDTO(List<MemoryCardDeckDO> deckDOList, Long userId) {
        if (deckDOList == null || deckDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集 deckIds
        List<Long> deckIdList = deckDOList.stream()
            .map(MemoryCardDeckDO::getId)
            .collect(Collectors.toList());

        // 批量获取创建者信息
        Map<Long, UserDO> userMap = batchGetCreators(deckDOList);

        // 批量获取点赞和收藏状态
        Map<Long, Boolean> upvoteStatusMap = new HashMap<>();
        Set<Long> bookmarkedSet = new HashSet<>();
        if (userId != null) {
            for (Long deckId : deckIdList) {
                boolean hasUpvoted = upvoteService.getUpvoteStatus(deckId, Enums.ContentType.memory_card_deck, userId).getLiked();
                upvoteStatusMap.put(deckId, hasUpvoted);
            }
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, deckIdList, Enums.ContentType.memory_card);
            bookmarkedSet.addAll(bookmarkedIds);
        }

        // 批量获取统计数据
        Map<Long, ContentStatsDTO> statsMap = contentStatsDomainService.batchGetContentStats(
            Enums.ContentType.memory_card_deck, deckIdList);

        // 批量获取课程和节点信息
        NodeCourseInfo nodeCourseInfo = batchGetNodeAndCourse(deckDOList);

        // 批量获取用户在每个 deck 中学习的卡片数量
        Map<Long, Integer> studyingCardCountMap = new HashMap<>();
        if (userId != null) {
            studyingCardCountMap = userCardSrsDataService.countByUserAndDeckIds(userId, deckIdList);
        }
        final Map<Long, Integer> finalStudyingCardCountMap = studyingCardCountMap;

        return deckDOList.stream()
            .map(deck -> {
                DeckFullDTO dto = deckConverter.toFullDTO(deck);

                // 设置创建者
                UserDO creator = userMap.get(deck.getCreatorId());
                if (creator != null) {
                    dto.setCreator(userConverter.toBriefDTO(creator));
                } else {
                    log.warn("Cannot find creator with id: {}", deck.getCreatorId());
                }

                // 设置点赞和收藏状态
                if (userId != null) {
                    dto.setHasLiked(upvoteStatusMap.get(deck.getId()));
                    dto.setBookmarked(bookmarkedSet.contains(deck.getId()));
                } else {
                    dto.setBookmarked(false);
                }

                // 设置统计数据
                ContentStatsDTO stats = statsMap.get(deck.getId());
                if (stats != null) {
                    dto.setLikeCount(stats.getLikeCount() != null ? stats.getLikeCount() : 0);
                }

                // 设置课程和节点信息
                if (deck.getNodeId() != null) {
                    NodeBriefDTO node = nodeCourseInfo.nodeMap.get(deck.getNodeId());
                    if (node != null) {
                        dto.setNode(node);
                    }
                    Long courseId = nodeCourseInfo.nodeToCourseMap.get(deck.getNodeId());
                    if (courseId != null) {
                        dto.setCourseId(courseId);
                        CourseBriefDTO course = nodeCourseInfo.courseMap.get(courseId);
                        if (course != null) {
                            dto.setCourse(course);
                        }
                    }
                }

                // 设置用户学习的卡片数量
                dto.setStudyingCardCount(finalStudyingCardCountMap.getOrDefault(deck.getId(), 0));

                return dto;
            })
            .collect(Collectors.toList());
    }

    // ========== 私有辅助方法 ==========

    private Map<Long, UserDO> batchGetCreators(List<MemoryCardDeckDO> deckDOList) {
        Set<Long> creatorIds = deckDOList.stream()
            .map(MemoryCardDeckDO::getCreatorId)
            .collect(Collectors.toSet());
        return userDataService.getMapByIds(creatorIds);
    }

    private void fillNodeAndCourse(DeckFullDTO dto, Long nodeId) {
        if (nodeId == null) return;

        NodeDO nodeDO = nodeDataService.getById(nodeId);
        if (nodeDO != null) {
            NodeBriefDTO nodeDTO = new NodeBriefDTO();
            nodeDTO.setId(nodeDO.getId());
            nodeDTO.setName(nodeDO.getName());
            dto.setNode(nodeDTO);

            if (nodeDO.getCourseId() != null) {
                dto.setCourseId(nodeDO.getCourseId());
                CourseDO courseDO = courseDataService.getById(nodeDO.getCourseId());
                if (courseDO != null) {
                    dto.setCourse(courseConverter.toBriefDTO(courseDO));
                }
            }
        }
    }

    private NodeCourseInfo batchGetNodeAndCourse(List<MemoryCardDeckDO> deckDOList) {
        NodeCourseInfo info = new NodeCourseInfo();

        Set<Long> nodeIds = deckDOList.stream()
            .map(MemoryCardDeckDO::getNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (nodeIds.isEmpty()) {
            return info;
        }

        Map<Long, NodeDO> nodeDOMap = nodeDataService.getMapByIds(nodeIds);
        Set<Long> courseIds = new HashSet<>();

        for (NodeDO nodeDO : nodeDOMap.values()) {
            if (nodeDO.getCourseId() != null) {
                courseIds.add(nodeDO.getCourseId());
                info.nodeToCourseMap.put(nodeDO.getId(), nodeDO.getCourseId());
            }
            NodeBriefDTO nodeDTO = new NodeBriefDTO();
            nodeDTO.setId(nodeDO.getId());
            nodeDTO.setName(nodeDO.getName());
            info.nodeMap.put(nodeDO.getId(), nodeDTO);
        }

        if (!courseIds.isEmpty()) {
            Map<Long, CourseDO> courseDOMap = courseDataService.getMapByIds(courseIds);
            for (CourseDO courseDO : courseDOMap.values()) {
                info.courseMap.put(courseDO.getId(), courseConverter.toBriefDTO(courseDO));
            }
        }

        return info;
    }

    /**
     * 节点和课程信息的封装类
     */
    private static class NodeCourseInfo {
        Map<Long, CourseBriefDTO> courseMap = new HashMap<>();
        Map<Long, NodeBriefDTO> nodeMap = new HashMap<>();
        Map<Long, Long> nodeToCourseMap = new HashMap<>();
    }
}
