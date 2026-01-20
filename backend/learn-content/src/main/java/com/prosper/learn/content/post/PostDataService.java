
package com.prosper.learn.content.post;

import com.prosper.learn.shared.dataservice.AbstractDataService;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子数据服务，提供缓存功能
 */
@Slf4j
@Service
public class PostDataService extends AbstractDataService<PostDO, PostMapper, Long> {
    
    @Autowired
    private PostMapper postMapper;
    
    @Override
    protected PostMapper mapper() {
        return postMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "posts";
    }
    
    @Override
    protected String getEntityName() {
        return "Post";
    }
    
    @Override
    protected Long getEntityId(PostDO entity) {
        return entity.getId();
    }
    
    @Override
    protected PostDO getByIdFromMapper(PostMapper mapper, Long id) {
        return mapper.get(id);  // 注意PostMapper的方法名是get而不是getById
    }

    /**
     * 重写父类方法，抛出 POST_NOT_FOUND 而不是通用的 NOT_FOUND
     */
    @Override
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
    
    @Override
    protected List<PostDO> getByIdsFromMapper(PostMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }
    
    @Override
    protected Map<Long, PostDO> getMapByIdsFromMapper(PostMapper mapper, Collection<Long> ids) {
        return mapper.getMapByIds(ids);
    }
    
    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(5);  // 帖子内容变化频繁，较短缓存时间
    }

    @Override
    protected int deleteByIdFromMapper(PostMapper mapper, Long id) {
        return 0;
    }

    /**
     * 更新帖子并清除缓存
     */
    @CacheEvict(value = "posts", key = "#post.id")
    public void update(PostDO post) {
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("Post or post ID cannot be null");
        }
        
        try {
            postMapper.update(post);
            log.debug("Updated post {}", post.getId());
        } catch (Exception e) {
            log.error("Error updating post: {}", post.getId(), e);
            throw StatusCode.DATABASE_ERROR.exception(e);
        }
    }
    
// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 更新帖子分数并清除缓存
//     */
//    @CacheEvict(value = "posts", key = "#id")
//    public boolean updateScore(long id, double score) {
//        try {
//            int result = postMapper.updateScore(id, score);
//            return result > 0;
//        } catch (Exception e) {
//            log.error("Error updating post score: {}", id, e);
//            throw ErrorCode.DATABASE_ERROR.exception(e);
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:17)

    /**
     * 统计活跃文章数量
     */
    public Long countActiveArticles() {
        return postMapper.countActiveArticles();
    }

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
     * 根据节点、创建者和状态筛选帖子列表（支持分页）
     */
    public List<PostDO> getListByNodeAndCreator(Long nodeId, Long creatorId, Long lastId, Byte state, int limit) {
        if (lastId == null || lastId == 0) {
            lastId = Long.MAX_VALUE;
        }
        return postMapper.getListByNodeAndCreatorWithPagination(nodeId, creatorId, lastId, state, limit);
    }

    /**
     * 插入帖子
     */
    public void insert(PostDO post) {
        postMapper.insert(post);
    }
    
    /**
     * 根据用户、类型和状态获取帖子列表
     */
    public List<PostDO> getPostsByUser(long userId, int type, Long lastId, Byte state, int limit) {
        return postMapper.getPostsByUser(userId, type, lastId, state, limit);
    }

    /**
     * 根据节点和分页获取帖子列表
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
     * 根据状态获取帖子列表
     */
    public List<PostDO> getListByState(byte state, int limit) {
        return postMapper.getListByState(state, limit);
    }

    /**
     * 根据状态获取帖子列表（支持分页）
     */
    public List<PostDO> getListByState(byte state, Long lastId, int limit) {
        if (lastId == null || lastId == 0) {
            return postMapper.getListByState(state, limit);
        }
        return postMapper.getListByStateWithPagination(state, lastId, limit);
    }

    /**
     * 根据节点和分数获取帖子列表
     */
    public List<PostDO> getListByNodeAndScore(long nodeId, int limit, byte state) {
        return postMapper.getListByNodeAndScore(nodeId, limit, state);
    }

// --注释掉检查 START (2025/12/10 11:17):
//    /**
//     * 根据节点和分页获取帖子列表
//     */
//    public List<PostDO> getListByLastId(long nodeId, long lastId, int limit, Byte state) {
//        return postMapper.getListByLastId(nodeId, lastId, limit, state);
//    }
// --注释掉检查 STOP (2025/12/10 11:17)

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
     * 封禁帖子（违规封禁）
     */
    @CacheEvict(value = "posts", key = "#id")
    public int ban(long id) {
        return postMapper.updateState(id, Enums.ContentState.BANNED.value());
    }

    /**
     * 封禁帖子（违规封禁，带原因）
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

    /**
     * 根据类型和状态查询帖子列表（分页）
     */
    public List<PostDO> getPostsByTypeAndState(Integer type, Byte state, Long lastId, int limit) {
        return postMapper.getPostsByTypeAndState(type, state, lastId, limit);
    }
}