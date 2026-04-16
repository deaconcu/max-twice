package com.prosper.learn.application.assembler;

import com.prosper.learn.application.converter.BookmarkConverter;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.MemoryCardDeckConverter;
import com.prosper.learn.application.converter.RoleConverter;
import com.prosper.learn.application.dto.response.bookmark.BookmarkDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.deck.DeckFullDTO;
import com.prosper.learn.application.dto.response.post.PostDetailDTO;
import com.prosper.learn.application.dto.response.role.RoleBriefDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.role.RoleDO;
import com.prosper.learn.content.role.RoleDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.bookmark.BookmarkDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bookmark DTO 组装器
 * 负责将 BookmarkDO 转换为带关联对象的 BookmarkDTO
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookmarkAssembler {

    private final BookmarkConverter bookmarkConverter;
    private final RoleDataService roleDataService;
    private final RoleConverter roleConverter;
    private final RoadmapDataService roadmapDataService;
    private final RoadmapAssembler roadmapAssembler;
    private final CourseDataService courseDataService;
    private final CourseConverter courseConverter;
    private final PostDataService postDataService;
    private final PostAssembler postAssembler;
    private final MemoryCardDeckDataService deckDataService;
    private final MemoryCardDeckConverter deckConverter;
    private final NodeDataService nodeDataService;

    /**
     * 转换收藏列表为带关联对象的 DTO
     */
    public List<BookmarkDTO<Object>> toDTO(List<BookmarkDO> bookmarks, Enums.ContentType contentType, Long userId) {
        if (bookmarks == null || bookmarks.isEmpty()) {
            return List.of();
        }

        return switch (contentType) {
            case role -> toRoleBookmarks(bookmarks);
            case roadmap -> toRoadmapBookmarks(bookmarks);
            case course -> toCourseBookmarks(bookmarks);
            case post -> toPostBookmarks(bookmarks);
            case memory_card_deck -> toDeckBookmarks(bookmarks);
            default -> bookmarkConverter.toDTO(bookmarks);
        };
    }

    // ========== 私有辅助方法 ==========

    private List<BookmarkDTO<Object>> toRoleBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<RoleDO> roleDOList = roleDataService.getByIds(objectIds);
        Map<Long, RoleBriefDTO> roleBriefDTOMap = roleDOList.stream()
            .collect(Collectors.toMap(RoleDO::getId, roleConverter::toBriefDTO));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(roleBriefDTOMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    private List<BookmarkDTO<Object>> toRoadmapBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<RoadmapDO> roadmaps = roadmapDataService.getByIds(objectIds);
        List<RoadmapBriefDTO> roadmapDTOs = roadmapAssembler.toBriefDTO(roadmaps);
        Map<Long, RoadmapBriefDTO> roadmapMap = roadmapDTOs.stream()
            .collect(Collectors.toMap(RoadmapBriefDTO::getId, r -> r));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(roadmapMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    private List<BookmarkDTO<Object>> toCourseBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<CourseDO> courses = courseDataService.getByIds(objectIds);
        List<CourseBriefDTO> courseDTOs = courseConverter.toBriefDTO(courses);
        Map<Long, CourseBriefDTO> courseMap = courseDTOs.stream()
            .collect(Collectors.toMap(CourseBriefDTO::getId, c -> c));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(courseMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    private List<BookmarkDTO<Object>> toPostBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<PostDO> posts = postDataService.getByIds(objectIds);
        List<PostDetailDTO> postDTOs = postAssembler.toDetailDTO(posts);
        Map<Long, PostDetailDTO> postMap = postDTOs.stream()
            .collect(Collectors.toMap(PostDetailDTO::getId, p -> p));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(postMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    private List<BookmarkDTO<Object>> toDeckBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<MemoryCardDeckDO> decks = deckDataService.getByIds(objectIds);

        // 批量获取节点名称作为卡片组名称
        Set<Long> nodeIds = decks.stream()
            .map(MemoryCardDeckDO::getNodeId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, NodeDO> nodeMap = nodeIds.isEmpty() ? Map.of() : nodeDataService.getMapByIds(nodeIds);

        // 转换为 DTO 并填充名称
        Map<Long, DeckFullDTO> deckMap = decks.stream()
            .collect(Collectors.toMap(MemoryCardDeckDO::getId, deck -> {
                DeckFullDTO dto = deckConverter.toFullDTO(deck);
                // 使用节点名称作为卡片组名称
                if (deck.getNodeId() != null) {
                    NodeDO node = nodeMap.get(deck.getNodeId());
                    if (node != null) {
                        dto.setName(node.getName());
                    }
                }
                return dto;
            }));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(deckMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }
}
