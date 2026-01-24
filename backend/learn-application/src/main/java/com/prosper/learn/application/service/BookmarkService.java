package com.prosper.learn.application.service;

import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.interaction.bookmark.BookmarkDataService;
import com.prosper.learn.interaction.bookmark.BookmarkDO;
import com.prosper.learn.interaction.bookmark.BookmarkDomainService;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkDomainService bookmarkDomainService;
    private final BookmarkDataService bookmarkDataService;
    private final CourseDataService courseDataService;
    private final ProfessionDataService professionDataService;
    private final RoadmapDataService roadmapDataService;
    private final PostDataService postDataService;
    private final MemoryCardDataService memoryCardDataService;

    /**
     * 切换收藏状态
     */
    public boolean toggleBookmark(long userId, long objectId, Enums.ContentType contentType) {
        // 验证对象存在并获取 parentId
        Long parentId = validateAndGetParentId(objectId, contentType);

        // 调用 domain 层切换收藏
        return bookmarkDomainService.toggleBookmark(userId, objectId, contentType, parentId);
    }

    /**
     * 获取用户某类型的收藏列表（分页）
     */
    public List<BookmarkDO> getUserBookmarks(long userId, Enums.ContentType contentType, long lastId, int limit) {
        return bookmarkDataService.listByUserAndLastId(userId, contentType.value(), lastId, limit);
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
            case profession -> {
                professionDataService.validateAndGet(objectId);
                yield null;
            }
            case roadmap -> {
                RoadmapDO roadmap = roadmapDataService.validateAndGet(objectId);
                yield roadmap.getProfessionId();
            }
            case course -> {
                courseDataService.validateAndGet(objectId);
                yield null;
            }
            case post -> {
                PostDO post = postDataService.validateAndGet(objectId);
                yield post.getNodeId();
            }
            case memory_card -> {
                MemoryCardDO card = memoryCardDataService.validateAndGet(objectId);
                yield card.getDeckId();
            }
            default -> throw new IllegalArgumentException("不支持的内容类型");
        };
    }
}
