package com.prosper.learn.content.post;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 帖子数据服务
 * 负责帖子数据的 CRUD 和缓存管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostDataService {

    private final PostMapper postMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据ID查询帖子
     */
    @Cacheable(value = "posts", key = "#id", unless = "#result == null")
    public PostDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return postMapper.get(id);
    }

    /**
     * 批量根据ID查询帖子
     */
    public List<PostDO> getByIds(Collection<Long> ids) {
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
        return postMapper.getByIds(validIds);
    }

    /**
     * 批量根据ID查询帖子并转为Map
     */
    public Map<Long, PostDO> getMapByIds(Collection<Long> ids) {
        return getByIds(ids).stream()
                .collect(Collectors.toMap(PostDO::getId, Function.identity()));
    }

    /**
     * 统计活跃文章数量
     */
    public Long countActiveArticles() {
        return postMapper.countActiveArticles();
    }

    /**
     * 检查节点下是否存在指定用户的帖子
     */
    public boolean existPost(long nodeId, long creatorId) {
        Long c = postMapper.countPostsByNodeAndCreator(nodeId, creatorId);
        return c != null && c > 0;
    }

    /**
     * 根据节点和创建者获取帖子列表（排除已删除状态）
     */
    public List<PostDO> getListByNodeAndCreator(long nodeId, long creatorId) {
        return postMapper.getListByNodeAndCreator(nodeId, creatorId, Enums.ContentState.BANNED.value());
    }

    /**
     * 高级筛选帖子列表
     */
    public List<PostDO> listByFilter(Long nodeId, Long creatorId, Long lastId, int limit) {
        return postMapper.listByFilter(nodeId, creatorId, lastId, limit);
    }

    /**
     * 根据用户、类型和状态获取帖子列表
     */
    public List<PostDO> getPostsByUser(long userId, int type, Long lastId, Byte state, int limit) {
        return postMapper.getPostsByUser(userId, type, lastId, state, limit);
    }

    /**
     * 根据节点和分页获取帖子列表（按分数排序）
     */
    public List<PostDO> getListByNodeAndScoreAndPaginated(long nodeId, double score, long offsetId, int limit, byte type) {
        return postMapper.getListByNodeAndScoreAndPaginated(nodeId, score, offsetId, limit, type);
    }

    /**
     * 根据节点获取帖子列表
     */
    public List<PostDO> getListByNode(long nodeId, int limit, byte type) {
        return postMapper.getListByNode(nodeId, limit, type);
    }

    /**
     * 根据状态获取帖子列表（支持分页）
     */
    public List<PostDO> listByState(Byte state, Long lastId, int limit) {
        return postMapper.listByState(state, lastId, limit);
    }

    /**
     * 根据节点和分数获取帖子列表
     */
    public List<PostDO> getListByNodeAndScore(long nodeId, int limit, byte state) {
        return postMapper.getListByNodeAndScore(nodeId, limit, state);
    }

    /**
     * 根据类型和状态查询帖子列表（分页）
     */
    public List<PostDO> getPostsByTypeAndState(Integer type, Byte state, Long lastId, int limit) {
        return postMapper.getPostsByTypeAndState(type, state, lastId, limit);
    }

    // ==================== 验证方法 ====================

    /**
     * 验证帖子ID并获取帖子
     */
    public PostDO validateAndGet(Long postId) {
        if (postId == null || postId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子ID无效");
        }
        PostDO post = getById(postId);
        if (post == null) {
            throw StatusCode.POST_NOT_FOUND.exception();
        }
        return post;
    }

    // ==================== 写入方法 ====================

    /**
     * 插入帖子
     */
    public void insert(PostDO post) {
        postMapper.insert(post);
    }

    /**
     * 更新帖子
     */
    @CacheEvict(value = "posts", key = "#post.id")
    public void update(PostDO post) {
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("Post or post ID cannot be null");
        }
        postMapper.update(post);
    }

    /**
     * 拒绝帖子（审核不通过）
     */
    @CacheEvict(value = "posts", key = "#id")
    public int reject(long id) {
        return postMapper.updateState(id, Enums.ContentState.REJECTED.value());
    }

    /**
     * 拒绝帖子（审核不通过，带原因）
     */
    @CacheEvict(value = "posts", key = "#id")
    public int reject(long id, String reason) {
        return postMapper.updateStateWithReason(id, Enums.ContentState.REJECTED.value(), reason);
    }

    /**
     * 封禁帖子
     */
    @CacheEvict(value = "posts", key = "#id")
    public int ban(long id) {
        return postMapper.updateState(id, Enums.ContentState.BANNED.value());
    }

    /**
     * 封禁帖子（带原因）
     */
    @CacheEvict(value = "posts", key = "#id")
    public int ban(long id, String reason) {
        return postMapper.updateStateWithReason(id, Enums.ContentState.BANNED.value(), reason);
    }

    /**
     * 软删除帖子
     */
    @CacheEvict(value = "posts", key = "#id")
    public int softDelete(long id) {
        return postMapper.softDelete(id);
    }
}
