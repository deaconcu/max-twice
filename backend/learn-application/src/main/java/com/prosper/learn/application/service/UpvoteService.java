package com.prosper.learn.application.service;

import com.prosper.learn.application.dto.response.UpvoteStatusDTO;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.interaction.comment.CommentDO;
import com.prosper.learn.interaction.comment.CommentDataService;
import com.prosper.learn.interaction.upvote.UpvoteDO;
import com.prosper.learn.interaction.upvote.UpvoteDomainService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.shared.domain.event.content.voting.*;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 点赞应用服务
 *
 * 负责协调跨领域逻辑、事件发布
 *
 * 核心功能：
 * - 帖子点赞（支持多种点赞类型：twice, like）
 * - 评论点赞
 * - 路线图投票
 * - 记忆卡片组点赞
 * - 点赞状态查询
 */
@Service
@RequiredArgsConstructor
public class UpvoteService {

    private final UpvoteDomainService upvoteDomainService;
    private final PostDataService postDataService;
    private final NodeDataService nodeDataService;
    private final CommentDataService commentDataService;
    private final RoadmapDataService roadmapDataService;
    private final ProfessionDataService professionDataService;
    private final MemoryCardDeckDataService deckDataService;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Command 方法（写操作）==========

    /**
     * 帖子点赞
     *
     * 支持多种点赞类型：twice, like
     * 包含点赞、取消点赞、切换点赞类型的完整逻辑
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param postId 帖子ID
     * @param user 用户
     * @param type 点赞类型（1-twice, 2-like）
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvotePost(long postId, UserDO user, int type) {
        // 验证和获取帖子对象
        PostDO postDO = postDataService.validateAndGet(postId);

        // 验证点赞类型
        if (type != VoteType.twice.value() && type != VoteType.like.value()) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子仅支持 twice 或 like 点赞类型: " + type);
        }

        // 调用 DomainService 执行点赞逻辑
        UpvoteDomainService.UpvoteAction action = upvoteDomainService.upvote(
            user.getId(),
            postDO.getId(),
            ContentType.post.value(),
            type
        );

        // 根据操作类型发布相应事件
        switch (action.getActionType()) {
            case REMOVED:
                // 取消点赞
                if (type == VoteType.twice.value()) {
                    eventPublisher.publishEvent(new TwiceUpvoteCancelledEvent<>(
                        user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO
                    ));
                } else {
                    eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                        user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, postDO.getNodeId()
                    ));
                }
                break;

            case ADDED:
                // 新增点赞
                if (type == VoteType.twice.value()) {
                    eventPublisher.publishEvent(new TwiceUpvotedEvent<>(
                        user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO
                    ));
                } else {
                    eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                        user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, postDO.getNodeId()
                    ));
                }
                break;

            case SWITCHED:
                // 切换点赞类型
                eventPublisher.publishEvent(new UpvoteTypeSwitchedEvent<>(
                    user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, action.getOldType(), type
                ));
                break;
        }
    }

    /**
     * 评论点赞
     *
     * 支持评论的点赞和取消点赞
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param commentId 评论ID
     * @param user 用户对象
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvoteComment(long commentId, UserDO user) {
        // 验证和获取评论对象
        CommentDO commentDO = commentDataService.validateAndGet(commentId);

        // 调用 DomainService 执行点赞/取消操作
        boolean added = upvoteDomainService.toggleUpvote(
            user.getId(),
            commentDO.getId(),
            ContentType.comment.value()
        );

        // 获取 nodeId（跨域逻辑）
        long nodeId = getNodeIdFromComment(commentDO);

        // 发布相应事件
        if (added) {
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), commentDO.getId(), ContentType.comment,
                commentDO.getCreatorId(), commentDO, nodeId
            ));
        } else {
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), commentDO.getId(), ContentType.comment,
                commentDO.getCreatorId(), commentDO, nodeId
            ));
        }
    }

    /**
     * 路线图投票
     *
     * 支持路线图的投票和取消投票
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param roadmapId 路线图ID
     * @param user 用户对象
     * @return true表示投票成功，false表示取消投票
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public boolean upvoteRoadmap(long roadmapId, UserDO user) {
        // 验证和获取路线图对象
        RoadmapDO roadmapDO = roadmapDataService.validateAndGet(roadmapId);

        // 调用 DomainService 执行投票/取消操作
        boolean added = upvoteDomainService.toggleUpvote(
            user.getId(),
            roadmapDO.getId(),
            ContentType.roadmap.value()
        );

        // 发布相应事件
        if (added) {
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), roadmapDO.getId(), ContentType.roadmap,
                roadmapDO.getCreatorId(), roadmapDO, roadmapDO.getProfessionId()
            ));
        } else {
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), roadmapDO.getId(), ContentType.roadmap,
                roadmapDO.getCreatorId(), roadmapDO, roadmapDO.getProfessionId()
            ));
        }

        return added;
    }

    /**
     * 记忆卡片组点赞
     *
     * 支持记忆卡片组的点赞和取消点赞
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param deckId 卡片组ID
     * @param user 用户对象
     * @return true表示点赞成功，false表示取消点赞
     * @throws BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public boolean upvoteMemoryCardDeck(long deckId, UserDO user) {
        // 验证和获取卡片组对象
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 调用 DomainService 执行点赞/取消操作
        boolean added = upvoteDomainService.toggleUpvote(
            user.getId(),
            deck.getId(),
            ContentType.memory_card_deck.value()
        );

        // 发布相应事件
        if (added) {
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), deck.getId(), ContentType.memory_card_deck,
                deck.getCreatorId(), deck, deck.getNodeId()
            ));
        } else {
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), deck.getId(), ContentType.memory_card_deck,
                deck.getCreatorId(), deck, deck.getNodeId()
            ));
        }

        return added;
    }

    // ========== Query 方法（读操作）==========

// --注释掉检查 START (2025/12/10 11:26):
//    /**
//     * 检查用户是否已经给指定内容投过票
//     */
//    public boolean hasUpvoted(long contentId, ContentType contentType, long userId) {
//        return upvoteDomainService.hasUpvoted(contentId, contentType.value(), userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:26)

// --注释掉检查 START (2025/12/10 11:26):
//    /**
//     * 批量检查用户对指定内容的投票状态
//     * @return 已投票的内容ID集合
//     */
//    public Set<Long> getUpvotedIds(List<Long> contentIds, ContentType contentType, long userId) {
//        return upvoteDomainService.getUpvotedIds(contentIds, contentType.value(), userId);
//    }
// --注释掉检查 STOP (2025/12/10 11:26)

    /**
     * 获取用户对指定对象的点赞状态
     *
     * @param objectId 对象ID
     * @param contentType 对象类型
     * @param userId 用户ID
     * @return 用户点赞状态DTO
     */
    public UpvoteStatusDTO getUpvoteStatus(Long objectId, ContentType contentType, long userId) {
        // 参数验证
        if (objectId == null || objectId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        if (userId <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }

        // 查询用户点赞记录
        UpvoteDO upvoteDO = upvoteDomainService.getUpvoteRecord(userId, objectId, contentType.value());

        // 构建用户点赞状态DTO
        boolean twiceUpvoted = false;
        boolean likeUpvoted = false;

        if (upvoteDO != null) {
            // 帖子特殊处理：支持 twice 和 like 分别统计
            if (contentType == ContentType.post) {
                twiceUpvoted = upvoteDO.getType() == VoteType.twice.value();
                likeUpvoted = upvoteDO.getType() == VoteType.like.value();
            } else {
                // 其他内容类型只有 like 统计
                likeUpvoted = true;
            }
        }

        return UpvoteStatusDTO.builder()
                .twiceUpvoted(twiceUpvoted)
                .likeUpvoted(likeUpvoted)
                .build();
    }

    // ========== Private 辅助方法 ==========

    /**
     * 获取评论对应的节点ID（跨域逻辑）
     */
    private long getNodeIdFromComment(CommentDO commentDO) {
        if (commentDO.getObjectType() == ContentType.node.value()) {
            return commentDO.getObjectId();
        } else if (commentDO.getObjectType() == ContentType.post.value()) {
            PostDO postDO = postDataService.validateAndGet(commentDO.getObjectId());
            return postDO.getNodeId();
        } else {
            throw StatusCode.INVALID_PARAMETER.exception("无效的评论类型: " + commentDO.getObjectType());
        }
    }
}
