package com.prosper.learn.domain.service.data;

import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.mapper.CommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论数据服务，提供缓存功能
 */
@Slf4j
@Service
public class CommentDataService extends AbstractDataService<CommentDO, CommentMapper, Long> {
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Override
    protected CommentMapper mapper() {
        return commentMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "comments";
    }
    
    @Override
    protected String getEntityName() {
        return "Comment";
    }
    
    @Override
    protected Long getEntityId(CommentDO entity) {
        return entity.getId();
    }
    
    @Override
    protected CommentDO getByIdFromMapper(CommentMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<CommentDO> getByIdsFromMapper(CommentMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids.stream().collect(Collectors.toList()));
    }
    
    @Override
    protected Map<Long, CommentDO> getMapByIdsFromMapper(CommentMapper mapper, Collection<Long> ids) {
        return getByIdsFromMapper(mapper, ids).stream()
                .collect(Collectors.toMap(CommentDO::getId, Function.identity()));
    }
    
    @Override
    protected Duration getCacheTtl() {
        return Duration.ofMinutes(5);  // 评论内容变化频繁，较短缓存时间
    }
    
    /**
     * 更新评论并清除缓存
     */
    @CacheEvict(value = "comments", key = "#comment.id")
    public void update(CommentDO comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("Comment or comment ID cannot be null");
        }
        
        try {
            commentMapper.update(comment);
            log.debug("Updated comment {}", comment.getId());
        } catch (Exception e) {
            log.error("Error updating comment: {}", comment.getId(), e);
            throw new RuntimeException("Failed to update comment: " + comment.getId(), e);
        }
    }
}