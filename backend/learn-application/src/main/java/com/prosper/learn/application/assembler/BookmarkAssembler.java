package com.prosper.learn.application.assembler;

import com.prosper.learn.application.converter.BookmarkConverter;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.PostConverter;
import com.prosper.learn.application.converter.ProfessionConverter;
import com.prosper.learn.application.dto.response.bookmark.BookmarkDTO;
import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.post.PostSummaryDTO;
import com.prosper.learn.application.dto.response.profession.ProfessionBriefDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.bookmark.BookmarkDO;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final ProfessionDataService professionDataService;
    private final ProfessionConverter professionConverter;
    private final RoadmapDataService roadmapDataService;
    private final RoadmapAssembler roadmapAssembler;
    private final CourseDataService courseDataService;
    private final CourseConverter courseConverter;
    private final PostDataService postDataService;
    private final PostConverter postConverter;
    private final MemoryCardDataService memoryCardDataService;
    private final CardAssembler cardAssembler;
    private final UserDataService userDataService;

    /**
     * 转换收藏列表为带关联对象的 DTO
     */
    public List<BookmarkDTO<Object>> toDTO(List<BookmarkDO> bookmarks, Enums.ContentType contentType, Long userId) {
        if (bookmarks == null || bookmarks.isEmpty()) {
            return List.of();
        }

        return switch (contentType) {
            case profession -> toProfessionBookmarks(bookmarks);
            case roadmap -> toRoadmapBookmarks(bookmarks);
            case course -> toCourseBookmarks(bookmarks);
            case post -> toPostBookmarks(bookmarks);
            case memory_card -> toCardBookmarks(bookmarks, userId);
            default -> bookmarkConverter.toDTO(bookmarks);
        };
    }

    // ========== 私有辅助方法 ==========

    private List<BookmarkDTO<Object>> toProfessionBookmarks(List<BookmarkDO> bookmarks) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<ProfessionDO> professions = professionDataService.getByIds(objectIds);
        Map<Long, ProfessionBriefDTO> professionMap = professions.stream()
            .collect(Collectors.toMap(ProfessionDO::getId, professionConverter::toBriefDTO));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(professionMap.get(bookmark.getObjectId()));
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
        List<PostSummaryDTO> postDTOs = postConverter.toSummaryDTO(posts);
        Map<Long, PostSummaryDTO> postMap = postDTOs.stream()
            .collect(Collectors.toMap(PostSummaryDTO::getId, p -> p));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(postMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    private List<BookmarkDTO<Object>> toCardBookmarks(List<BookmarkDO> bookmarks, Long userId) {
        List<Long> objectIds = bookmarks.stream().map(BookmarkDO::getObjectId).collect(Collectors.toList());
        List<MemoryCardDO> cards = memoryCardDataService.getByIds(objectIds);

        // 获取用户对象用于 CardAssembler
        UserDO user = userId != null ? userDataService.getById(userId) : null;

        // 批量转换卡片
        List<CardWithSrsDTO> cardDTOs = cardAssembler.toCardViewWithSrs(cards, user);
        Map<Long, CardWithSrsDTO> cardMap = cardDTOs.stream()
            .collect(Collectors.toMap(CardWithSrsDTO::getId, c -> c));

        List<BookmarkDTO<Object>> result = new ArrayList<>();
        for (BookmarkDO bookmark : bookmarks) {
            BookmarkDTO<Object> dto = bookmarkConverter.toDTO(bookmark);
            dto.setObject(cardMap.get(bookmark.getObjectId()));
            result.add(dto);
        }
        return result;
    }
}
