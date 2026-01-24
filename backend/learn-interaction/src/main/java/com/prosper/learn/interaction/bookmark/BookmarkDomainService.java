package com.prosper.learn.interaction.bookmark;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkDomainService {

    private final BookmarkDataService bookmarkDataService;
    private final ApplicationEventPublisher eventPublisher;
    private final SystemProperties systemProperties;

    /**
     * 切换收藏状态（toggle）
     */
    public boolean toggleBookmark(long userId, long objectId, Enums.ContentType contentType, Long parentId) {
        BookmarkDO existing = bookmarkDataService.getByUserAndObject(userId, objectId, contentType.value());

        if (existing == null) {
            // 检查收藏数量限制
            int maxBookmarks = systemProperties.getBookmark().getMaxBookmarksPerType();
            int count = bookmarkDataService.countByUser(userId, contentType.value());
            if (count >= maxBookmarks) {
                throw new RuntimeException("收藏数量已达上限（每类型最多" + maxBookmarks + "条）");
            }

            // 添加收藏
            bookmarkDataService.create(userId, objectId, contentType.value(), parentId);
            eventPublisher.publishEvent(new ContentBookmarkedEvent(userId, objectId, contentType));
            return true;
        } else {
            // 取消收藏
            bookmarkDataService.delete(existing.getId());
            eventPublisher.publishEvent(new ContentUnbookmarkedEvent(userId, objectId, contentType));
            return false;
        }
    }

    /**
     * 批量检查收藏状态
     */
    public List<Long> getBookmarkedIds(long userId, List<Long> ids, Enums.ContentType contentType) {
        return bookmarkDataService.getBookmarkedIds(userId, ids, contentType.value());
    }

    /**
     * 获取用户某类型的所有收藏ID
     */
    public List<Long> getBookmarkedIdsByType(long userId, Enums.ContentType contentType) {
        return bookmarkDataService.getBookmarkedIdsByType(userId, contentType.value());
    }
}
