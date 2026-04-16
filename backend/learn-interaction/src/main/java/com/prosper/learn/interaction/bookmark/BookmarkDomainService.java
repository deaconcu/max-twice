package com.prosper.learn.interaction.bookmark;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.content.interaction.ContentBookmarkedEvent;
import com.prosper.learn.shared.domain.event.content.interaction.ContentUnbookmarkedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkDomainService {

    private static final int MAX_BOOKMARKS_PER_TYPE = 1000;

    private final BookmarkDataService bookmarkDataService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 切换收藏状态（toggle）
     */
    public boolean toggleBookmark(long userId, long objectId, Enums.ContentType contentType, Long parentId) {
        BookmarkDO existing = bookmarkDataService.getByUserAndObject(userId, objectId, contentType.value());

        if (existing == null) {
            // 检查收藏数量限制
            int count = bookmarkDataService.countByUser(userId, contentType.value());
            if (count >= MAX_BOOKMARKS_PER_TYPE) {
                throw new RuntimeException("收藏数量已达上限（每类型最多" + MAX_BOOKMARKS_PER_TYPE + "条）");
            }

            // 添加收藏
            bookmarkDataService.create(userId, objectId, contentType.value(), parentId);
            eventPublisher.publishEvent(new ContentBookmarkedEvent(userId, objectId, contentType));
            log.debug("收藏 新增: userId={}，objectId={}，contentType={}", userId, objectId, contentType);
            return true;
        } else {
            // 取消收藏
            bookmarkDataService.delete(existing.getId());
            eventPublisher.publishEvent(new ContentUnbookmarkedEvent(userId, objectId, contentType));
            log.debug("收藏 取消: userId={}，objectId={}，contentType={}", userId, objectId, contentType);
            return false;
        }
    }

    /**
     * 批量检查收藏状态
     */
    public List<Long> getBookmarkedIds(long userId, List<Long> ids, Enums.ContentType contentType) {
        return bookmarkDataService.getBookmarkedIds(userId, ids, contentType.value());
    }

}
