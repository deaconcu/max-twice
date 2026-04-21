package com.twicemax.interaction.upvote;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 投票数据服务
 * 负责投票数据的 CRUD 和缓存管理
 *
 * 缓存策略：
 * - 缓存用户对特定对象的投票查询（高频调用，判断是否已点赞）
 * - 列表查询直接走数据库
 * - 写操作清除相关缓存
 */
@Service
@RequiredArgsConstructor
public class UpvoteDataService {

    private final UpvoteMapper upvoteMapper;

    // ==================== 查询方法 ====================

    /**
     * 获取用户对特定对象的投票
     */
    @Cacheable(value = "upvotes", key = "#userId + '_' + #objectId + '_' + #objectType", unless = "#result == null")
    public UpvoteDO getByUserAndObject(long userId, long objectId, int objectType) {
        return upvoteMapper.getByUserAndObject(userId, objectId, objectType);
    }

    /**
     * 获取用户的投票列表
     */
    public List<UpvoteDO> getList(long userId, List<Long> objectIds, int objectType) {
        if (objectIds == null || objectIds.isEmpty()) {
            return List.of();
        }
        return upvoteMapper.getList(userId, objectIds, objectType);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入投票记录
     */
    @CacheEvict(value = "upvotes", key = "#upvoteDO.userId + '_' + #upvoteDO.objectId + '_' + #upvoteDO.objectType")
    public void insert(UpvoteDO upvoteDO) {
        upvoteMapper.insert(upvoteDO);
    }

    /**
     * 更新投票记录
     */
    @CacheEvict(value = "upvotes", key = "#upvoteDO.userId + '_' + #upvoteDO.objectId + '_' + #upvoteDO.objectType")
    public void update(UpvoteDO upvoteDO) {
        upvoteMapper.update(upvoteDO);
    }

    /**
     * 删除投票记录
     */
    public void delete(long id) {
        UpvoteDO upvote = upvoteMapper.getById(id);
        if (upvote != null) {
            evictCache(upvote.getUserId(), upvote.getObjectId(), upvote.getObjectType());
            upvoteMapper.delete(id);
        }
    }

    // ==================== 缓存辅助方法 ====================

    @CacheEvict(value = "upvotes", key = "#userId + '_' + #objectId + '_' + #objectType")
    public void evictCache(long userId, long objectId, int objectType) {
        // 仅用于清除缓存
    }
}
