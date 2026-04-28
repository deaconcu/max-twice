package com.twicemax.application.service;

import com.twicemax.application.assembler.BookmarkAssembler;
import com.twicemax.application.dto.response.bookmark.BookmarkDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.content.course.CourseDataService;
import com.twicemax.content.post.PostDataService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.interaction.bookmark.BookmarkDataService;
import com.twicemax.interaction.bookmark.BookmarkDO;
import com.twicemax.interaction.bookmark.BookmarkDomainService;
import com.twicemax.memory.deck.MemoryCardDeckDO;
import com.twicemax.memory.deck.MemoryCardDeckDataService;
import com.twicemax.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkDomainService bookmarkDomainService;
    private final BookmarkDataService bookmarkDataService;
    private final BookmarkAssembler bookmarkAssembler;
    private final CourseDataService courseDataService;
    private final RoleDataService roleDataService;
    private final RoadmapDataService roadmapDataService;
    private final PostDataService postDataService;
    private final MemoryCardDeckDataService memoryCardDeckDataService;
    private final ContentVisibilityService contentVisibilityService;

    /**
     * 切换收藏状态
     */
    @Transactional
    public boolean toggleBookmark(long userId, long objectId, Enums.ContentType contentType) {
        // 验证内容可见性
        contentVisibilityService.validateVisibility(contentType, objectId, userId);

        // 验证对象存在并获取 parentId
        Long parentId = validateAndGetParentId(objectId, contentType);

        // 调用 domain 层切换收藏
        return bookmarkDomainService.toggleBookmark(userId, objectId, contentType, parentId);
    }

    /**
     * 获取用户某类型的收藏列表（游标分页，带关联对象）
     */
    public CursorPage<BookmarkDTO<Object>> getUserBookmarks(long userId, Enums.ContentType contentType, String cursor, int limit) {
        List<BookmarkDO> bookmarks = bookmarkDataService.listByUserAndLastId(userId, contentType.value(), Cursor.decode(cursor).id(), limit + 1);
        boolean hasMore = bookmarks.size() > limit;
        if (hasMore) {
            bookmarks = bookmarks.subList(0, limit);
        }
        List<BookmarkDTO<Object>> dtos = bookmarkAssembler.toDTO(bookmarks, contentType, userId);
        String nextCursor = (hasMore && !dtos.isEmpty()) ? Cursor.of(dtos.get(dtos.size() - 1).getId()).encode() : null;
        return CursorPage.of(dtos, hasMore, nextCursor);
    }

    /**
     * 批量检查收藏状态
     */
    public List<Long> getBookmarkedIds(long userId, List<Long> ids, Enums.ContentType contentType) {
        return bookmarkDomainService.getBookmarkedIds(userId, ids, contentType);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isBookmarked(long userId, long objectId, Enums.ContentType contentType) {
        return bookmarkDataService.isBookmarked(userId, objectId, contentType.value());
    }

    /**
     * 验证对象存在并获取 parentId
     */
    private Long validateAndGetParentId(long objectId, Enums.ContentType contentType) {
        return switch (contentType) {
            case role -> {
                roleDataService.validateAndGet(objectId);
                yield null;
            }
            case roadmap -> {
                RoadmapDO roadmap = roadmapDataService.validateAndGet(objectId);
                yield roadmap.getRoleId();
            }
            case course -> {
                courseDataService.validateAndGet(objectId);
                yield null;
            }
            case post -> {
                PostDO post = postDataService.validateAndGet(objectId);
                yield post.getNodeId();
            }
            case memory_card_deck -> {
                MemoryCardDeckDO deck = memoryCardDeckDataService.validateAndGet(objectId);
                yield deck.getNodeId();
            }
            default -> throw new IllegalArgumentException("不支持的内容类型");
        };
    }
}
