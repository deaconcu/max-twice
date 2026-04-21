package com.twicemax.interaction.follow;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 关注数据服务
 * 负责关注关系数据的 CRUD
 */
@Service
@RequiredArgsConstructor
public class FollowDataService {

    private final FollowMapper followMapper;

    // ==================== 查询方法 ====================

    /**
     * 获取关注关系
     */
    public FollowDO get(long followerId, long followeeId) {
        return followMapper.get(followerId, followeeId);
    }

    /**
     * 获取关注列表
     */
    public List<FollowDO> getList(long followerId, Long lastId, int limit) {
        return followMapper.getList(followerId, lastId, limit);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入关注关系
     */
    public void insert(Long followerId, Long followeeId) {
        followMapper.insert(followerId, followeeId);
    }

    /**
     * 取消关注
     */
    public void delete(long followerId, long followeeId) {
        followMapper.delete(followerId, followeeId);
    }
}
