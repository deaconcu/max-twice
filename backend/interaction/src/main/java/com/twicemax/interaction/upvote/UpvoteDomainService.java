package com.twicemax.interaction.upvote;

import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞领域服务
 * 只依赖 interaction 模块，处理点赞的核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpvoteDomainService {

    private final UpvoteDataService upvoteDataService;

    // ========== Command 方法 ==========

    /**
     * 点赞操作（核心逻辑）
     *
     * @param userId 用户ID
     * @param objectId 对象ID
     * @param objectType 对象类型
     * @param type 点赞类型
     * @return UpvoteAction 操作结果（ADDED/REMOVED/SWITCHED）
     */
    @Transactional
    public UpvoteAction upvote(Long userId, Long objectId, Integer objectType, String type) {
        // 查询现有点赞记录
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, objectId, objectType);

        // 场景1：如果已经是同类型点赞，则取消点赞
        if (upvoteDO != null && upvoteDO.getType().equals(type)) {
            upvoteDataService.delete(upvoteDO.getId());
            log.debug("点赞 取消: userId={}，objectId={}，objectType={}，type={}",
                userId, objectId, objectType, type);
            return new UpvoteAction(UpvoteActionType.REMOVED, type, null);
        }

        // 场景2：新增点赞
        if (upvoteDO == null) {
            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(objectId);
            upvoteDO.setObjectType(objectType);
            upvoteDO.setType(type);
            upvoteDataService.insert(upvoteDO);
            log.debug("点赞 新增: userId={}，objectId={}，objectType={}，type={}",
                userId, objectId, objectType, type);
            return new UpvoteAction(UpvoteActionType.ADDED, type, null);
        }

        // 场景3：切换点赞类型
        String oldType = upvoteDO.getType();
        upvoteDO.setType(type);
        upvoteDataService.update(upvoteDO);
        log.debug("点赞 切换类型: userId={}，objectId={}，objectType={}，oldType={}，newType={}",
            userId, objectId, objectType, oldType, type);
        return new UpvoteAction(UpvoteActionType.SWITCHED, type, oldType);
    }

    /**
     * 简单点赞/取消（不支持类型切换）
     * 用于 comment、roadmap、memory_card_deck 等只有一种点赞类型的内容
     *
     * @return true 表示点赞，false 表示取消点赞
     */
    @Transactional
    public boolean toggleUpvote(Long userId, Long objectId, Integer objectType) {
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, objectId, objectType);

        if (upvoteDO != null) {
            // 取消点赞
            upvoteDataService.delete(upvoteDO.getId());
            return false;
        } else {
            // 新增点赞
            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(userId);
            upvoteDO.setObjectId(objectId);
            upvoteDO.setObjectType(objectType);
            upvoteDO.setType(Enums.VoteType.LIKE.value()); // 评论、路线图、卡片组都使用 like 类型
            upvoteDataService.insert(upvoteDO);
            return true;
        }
    }

    // ========== Query 方法 ==========

    /**
     * 检查用户是否已经给指定内容投过票
     */
    public boolean hasUpvoted(Long contentId, Integer contentType, Long userId) {
        if (userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }
        if (contentId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, contentId, contentType);
        return upvoteDO != null;
    }

    /**
     * 批量检查用户对指定内容的投票状态
     * @return 已投票的内容ID集合
     */
    public Set<Long> getUpvotedIds(List<Long> contentIds, Integer contentType, Long userId) {
        if (contentIds == null || contentIds.isEmpty() || userId <= 0) {
            return new HashSet<>();
        }

        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, contentIds, contentType);
        return upvotes.stream()
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());
    }

    /**
     * 获取用户对指定对象的点赞记录
     */
    public UpvoteDO getUpvoteRecord(Long userId, Long objectId, Integer objectType) {
        return upvoteDataService.getByUserAndObject(userId, objectId, objectType);
    }

    // ========== 内部类：操作结果 ==========

    /**
     * 点赞操作结果
     */
    public static class UpvoteAction {
        private final UpvoteActionType actionType;
        private final String newType;
        private final String oldType;

        public UpvoteAction(UpvoteActionType actionType, String newType, String oldType) {
            this.actionType = actionType;
            this.newType = newType;
            this.oldType = oldType;
        }

        public UpvoteActionType getActionType() {
            return actionType;
        }

        public String getNewType() {
            return newType;
        }

        public String getOldType() {
            return oldType;
        }
    }

    /**
     * 点赞操作类型
     */
    public enum UpvoteActionType {
        ADDED,      // 新增点赞
        REMOVED,    // 取消点赞
        SWITCHED    // 切换类型
    }
}
