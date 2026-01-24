package com.prosper.learn.interaction.bookmark;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkDataService {

    private final BookmarkMapper bookmarkMapper;

    /**
     * 根据用户和对象查询收藏
     */
    public BookmarkDO getByUserAndObject(long userId, long objectId, int objectType) {
        return bookmarkMapper.getByUserAndObject(userId, objectId, objectType);
    }

    /**
     * 批量检查收藏状态
     */
    public List<Long> getBookmarkedIds(long userId, List<Long> ids, int objectType) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        return bookmarkMapper.getBookmarkedIds(userId, ids, objectType);
    }

    /**
     * 创建收藏
     */
    public BookmarkDO create(long userId, long objectId, int objectType, Long parentId) {
        BookmarkDO bookmark = new BookmarkDO();
        bookmark.setUserId(userId);
        bookmark.setObjectId(objectId);
        bookmark.setObjectType(objectType);
        bookmark.setParentId(parentId);
        bookmark.setCreatedAt(LocalDateTime.now());
        bookmarkMapper.insert(bookmark);
        return bookmark;
    }

    /**
     * 删除收藏
     */
    public void delete(long id) {
        bookmarkMapper.delete(id);
    }

    /**
     * 分页查询用户收藏（使用 lastId）
     */
    public List<BookmarkDO> listByUserAndLastId(long userId, int objectType, long lastId, int limit) {
        if (lastId == 0) {
            lastId = Long.MAX_VALUE;
        }
        return bookmarkMapper.listByUserAndLastId(userId, objectType, lastId, limit);
    }

    /**
     * 统计用户某类型收藏数量
     */
    public int countByUser(long userId, int objectType) {
        return bookmarkMapper.countByUser(userId, objectType);
    }

    /**
     * 获取用户某类型的所有收藏ID
     */
    public List<Long> getBookmarkedIdsByType(long userId, int objectType) {
        return bookmarkMapper.getBookmarkedIdsByType(userId, objectType);
    }

    /**
     * 检查是否已收藏
     */
    public boolean isBookmarked(long userId, long objectId, int objectType) {
        return bookmarkMapper.isBookmarked(userId, objectId, objectType);
    }
}
