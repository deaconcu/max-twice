package com.twicemax.interaction.comment;

import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论数据服务
 * 负责评论数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 只缓存单条查询 getById
 * - 列表查询直接走数据库
 * - 写操作清除相关缓存
 */
@Service
@RequiredArgsConstructor
public class CommentDataService {

    private final CommentMapper commentMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询评论
     */
    @Cacheable(value = "comments", key = "#id", unless = "#result == null")
    public CommentDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return commentMapper.getById(id);
    }

    /**
     * 批量根据ID查询评论
     */
    public List<CommentDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return new ArrayList<>();
        }
        return commentMapper.getByIds(validIds);
    }

    /**
     * 根据对象ID获取评论列表
     */
    public List<CommentDO> getByObjectId(long objectId, int type, int pageSize) {
        return commentMapper.getByObjectId(objectId, type, pageSize);
    }

    /**
     * 根据对象ID分页获取评论列表
     */
    public List<CommentDO> getByObjectIdPaginated(long objectId, int type, double score, long offsetId, int pageSize) {
        return commentMapper.getByObjectIdPaginated(objectId, type, score, offsetId, pageSize);
    }

    /**
     * 获取子评论列表
     */
    public List<CommentDO> getChildren(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return commentMapper.getChildren(ids);
    }

    /**
     * 根据主题获取评论列表
     */
    public List<CommentDO> getByTopic(long id, int pageSize) {
        return commentMapper.getByTopic(id, pageSize);
    }

    /**
     * 根据主题分页获取评论列表
     */
    public List<CommentDO> getByTopicPaginated(long id, double score, long offsetId, int pageSize) {
        return commentMapper.getByTopicPaginated(id, score, offsetId, pageSize);
    }

    /**
     * 根据状态获取评论列表
     */
    public List<CommentDO> listByState(Byte state, Long lastId, int limit) {
        return commentMapper.listByState(state, lastId, limit);
    }

    /**
     * 高级筛选评论列表
     */
    public List<CommentDO> listByFilter(Integer objectType, Long objectId, Long creatorId, Long lastId, int limit) {
        return commentMapper.listByFilter(objectType, objectId, creatorId, lastId, limit);
    }

    /**
     * 获取目标评论之前的评论（score更高的）
     */
    public List<CommentDO> getCommentsBeforeTarget(long objectId, int objectType, double score, long id, int count) {
        return commentMapper.getCommentsBeforeTarget(objectId, objectType, score, id, count);
    }

    /**
     * 获取目标评论及之后的评论（score更低或相等的）
     */
    public List<CommentDO> getCommentsFromTarget(long objectId, int objectType, double score, long id, int count) {
        return commentMapper.getCommentsFromTarget(objectId, objectType, score, id, count);
    }

    /**
     * 获取目标子评论之前的子评论（score更高的）
     */
    public List<CommentDO> getSubCommentsBeforeTarget(long parentCommentId, double score, long id, int count) {
        return commentMapper.getSubCommentsBeforeTarget(parentCommentId, score, id, count);
    }

    /**
     * 获取目标子评论及之后的子评论（score更低或相等的）
     */
    public List<CommentDO> getSubCommentsFromTarget(long parentCommentId, double score, long id, int count) {
        return commentMapper.getSubCommentsFromTarget(parentCommentId, score, id, count);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证并获取评论
     */
    public CommentDO validateAndGet(Long id) {
        if (id == null || id <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("评论ID无效");
        }
        CommentDO comment = getById(id);
        if (comment == null) {
            throw StatusCode.COMMENT_NOT_FOUND.exception();
        }
        return comment;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入评论
     */
    public void insert(CommentDO comment) {
        commentMapper.insert(comment);
    }

    /**
     * 更新评论
     */
    @CacheEvict(value = "comments", key = "#comment.id")
    public void update(CommentDO comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("Comment or comment ID cannot be null");
        }
        commentMapper.update(comment);
    }

    /**
     * 拒绝评论
     */
    @CacheEvict(value = "comments", key = "#id")
    public int reject(long id, String reason) {
        return commentMapper.updateStateWithReason(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁评论
     */
    @CacheEvict(value = "comments", key = "#id")
    public int ban(long id, String reason) {
        return commentMapper.updateStateWithReason(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 软删除评论
     */
    @CacheEvict(value = "comments", key = "#id")
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        return commentMapper.softDelete(id) > 0;
    }
}
