package com.prosper.learn.domain.service.data;

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
            throw new RuntimeException("Failed to update post: " + post.getId(), e);
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
            throw new RuntimeException("Failed to update post score: " + id, e);
        }
    }
}