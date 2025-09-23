package com.prosper.learn.domain.service.data;

import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.mapper.PostMapper;
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
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
    /**
     * 更新帖子分数并清除缓存
     */
    @CacheEvict(value = "posts", key = "#id")
    public boolean updateScore(long id, double score) {
        try {
            int result = postMapper.updateScore(id, score);
            return result > 0;
        } catch (Exception e) {
            log.error("Error updating post score: {}", id, e);
            throw ErrorCode.DATABASE_ERROR.exception(e);
        }
    }
    
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
     * 插入帖子
     */
    public void insert(PostDO post) {
        post.setTwice(0);
        post.setHelpful(0);
        post.setCommentCount(0);
        post.setCreatedAt(Utils.getLocalDateTime());
        post.setScore(0.0);
        postMapper.insert(post);
    }
    
    /**
     * 根据用户获取文章列表
     */
    public List<PostDO> getArticleListByUser(long userId, long lastId, int limit) {
        return postMapper.getArticleListByUser(userId, lastId, limit);
    }
    
    /**
     * 根据用户获取内容列表
     */
    public List<PostDO> getContentsListByUser(long userId, long lastId, int limit) {
        return postMapper.getContentsListByUser(userId, lastId, limit);
    }
    
    /**
     * 根据节点和分页获取帖子列表
     */
    public List<PostDO> getListByNodeAndScoreAndPaginated(Long nodeId, double score, Long offsetId, int limit, Integer type) {
        return postMapper.getListByNodeAndScoreAndPaginated(nodeId, score, offsetId, limit, type);
    }
    
    /**
     * 根据节点获取帖子列表
     */
    public List<PostDO> getListByNode(Long nodeId, int limit, Integer type) {
        return postMapper.getListByNode(nodeId, limit, type);
    }
    
    /**
     * 根据状态获取帖子列表
     */
    public List<PostDO> getListByState(Integer state, int limit) {
        return postMapper.getListByState(state, limit);
    }

    /**
     * 根据节点和分数获取帖子列表
     */
    public List<PostDO> getListByNodeAndScore(long nodeId, int limit, Integer state) {
        return postMapper.getListByNodeAndScore(nodeId, limit, state);
    }

    /**
     * 根据节点和分页获取帖子列表
     */
    public List<PostDO> getListByLastId(long nodeId, long lastId, int limit, Integer state) {
        return postMapper.getListByLastId(nodeId, lastId, limit, state);
    }
}