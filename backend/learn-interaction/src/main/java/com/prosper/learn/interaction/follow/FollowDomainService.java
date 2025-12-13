package com.prosper.learn.interaction.follow;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关注领域服务
 *
 * 只依赖 interaction 模块，处理关注的核心业务逻辑
 *
 * 职责：
 * - 关注/取消关注的业务逻辑
 * - 关注关系的查询
 * - 关注列表的获取
 */
@Service
@RequiredArgsConstructor
public class FollowDomainService {

    /** 默认分页大小常量 */
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final FollowDataService followDataService;

    // ========== Command 方法 ==========

    /**
     * 关注用户
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return true 表示关注成功，false 表示已经关注过（幂等）
     */
    @Transactional
    public boolean follow(Long followerId, Long followeeId) {
        validateFollowParams(followerId, followeeId);

        // 检查是否已经关注
        FollowDO existingFollow = followDataService.get(followerId, followeeId);
        if (existingFollow != null) {
            return false; // 已经关注过，幂等操作
        }

        // 创建关注记录
        followDataService.insert(followerId, followeeId);
        return true;
    }

    /**
     * 取消关注用户
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return true 表示取消关注成功，false 表示原本就没有关注（幂等）
     */
    @Transactional
    public boolean unfollow(Long followerId, Long followeeId) {
        validateFollowParams(followerId, followeeId);

        // 检查关注关系是否存在
        FollowDO existingFollow = followDataService.get(followerId, followeeId);
        if (existingFollow == null) {
            return false; // 原本就没有关注，幂等操作
        }

        // 删除关注记录
        followDataService.delete(followerId, followeeId);
        return true;
    }

    // ========== Query 方法 ==========

    /**
     * 检查用户是否关注了另一个用户
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @return true 表示已关注，false 表示未关注
     */
    public boolean isFollowing(Long followerId, Long followeeId) {
        validateUserId(followerId, "关注者ID");
        validateUserId(followeeId, "被关注者ID");

        FollowDO followDO = followDataService.get(followerId, followeeId);
        return followDO != null;
    }

    /**
     * 获取用户的关注列表
     *
     * @param userId 用户ID
     * @param lastCreateTime 最后一条记录的创建时间（用于分页）
     * @return 关注记录列表
     */
    public List<FollowDO> getFollowees(Long userId, LocalDateTime lastCreateTime) {
        validateUserId(userId, "用户ID");
        return followDataService.getList(userId, lastCreateTime, DEFAULT_PAGE_SIZE);
    }

// --注释掉检查 START (2025/12/10 11:12):
//    /**
//     * 获取关注记录
//     *
//     * @param followerId 关注者ID
//     * @param followeeId 被关注者ID
//     * @return 关注记录，如果不存在则返回 null
//     */
//    public FollowDO getFollowRecord(Long followerId, Long followeeId) {
//        validateUserId(followerId, "关注者ID");
//        validateUserId(followeeId, "被关注者ID");
//        return followDataService.get(followerId, followeeId);
//    }
// --注释掉检查 STOP (2025/12/10 11:12)

    // ========== Private 辅助方法 ==========

    /**
     * 验证用户ID有效性
     *
     * @param userId 用户ID
     * @param fieldName 字段名称（用于错误提示）
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当用户ID无效时抛出异常
     */
    private void validateUserId(Long userId, String fieldName) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception(fieldName + "无效: " + userId);
        }
    }

    /**
     * 验证关注参数有效性
     *
     * @param followerId 关注者ID
     * @param followeeId 被关注者ID
     * @throws com.prosper.learn.shared.domain.exception.BusinessException 当参数无效时抛出异常
     */
    private void validateFollowParams(Long followerId, Long followeeId) {
        validateUserId(followerId, "关注者ID");
        validateUserId(followeeId, "被关注者ID");

        if (followerId.equals(followeeId)) {
            throw ErrorCode.INVALID_PARAMETER.exception("不能关注自己");
        }
    }
}
