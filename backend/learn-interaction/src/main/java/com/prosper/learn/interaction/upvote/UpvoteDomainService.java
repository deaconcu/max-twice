package com.prosper.learn.interaction.upvote;

import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞服务
 * 
 * 负责管理系统中的点赞功能，包括：
 * - 帖子点赞（支持多种点赞类型：once, twice, helpful）
 * - 评论点赞
 * - 路线图投票
 * - 点赞状态查询和批量查询
 * 
 * 核心功能：
 * - 支持点赞/取消点赞/切换点赞类型
 * - 自动更新统计数据和分数
 * - 发送点赞通知消息
 * - Redis统计数据记录
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Service
@RequiredArgsConstructor
public class UpvoteDomainService {

    /** 点赞类型名称常量 */
    private static final String UPVOTE_TYPE_TWICE = "twice";
    private static final String UPVOTE_TYPE_LIKE = "like";

    /** 点赞数据访问接口 */
    private final UpvoteDataService upvoteDataService;
    
    /** 帖子数据访问接口 */
    private final PostDataService postDataService;

    /** 内容统计业务服务 */
    private final ContentStatsDomainService contentStatsDomainService;
    
    /** 事件发布器，用于发布点赞相关事件 */
    private final ApplicationEventPublisher eventPublisher;


    /**
     * 帖子点赞
     *
     * 支持多种点赞类型：twice, like
     * 包含点赞、取消点赞、切换点赞类型的完整逻辑
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param postDO 帖子对象（已验证存在）
     * @param user 用户
     * @param type 点赞类型（1-twice, 2-like）
     * @throws BusinessException 当参数无效时抛出异常
     */
    @Transactional
    public void upvotePost(PostDO postDO, UserDO user, int type) {
        // 验证点赞类型
        if (type != Enums.VoteType.twice.value() && type != Enums.VoteType.like.value()) {
            throw ErrorCode.INVALID_PARAMETER.exception("帖子仅支持 twice 或 like 点赞类型: " + type);
        }

        // 查询现有点赞记录
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(user.getId(), postDO.getId(), ContentType.post.value());

        // 场景1：如果已经是同类型点赞，则取消点赞
        if (upvoteDO != null && upvoteDO.getType() == type) {
            // 删除点赞记录
            upvoteDataService.delete(upvoteDO.getId());

            // 发布取消点赞事件
            if (type == Enums.VoteType.twice.value()) {
                eventPublisher.publishEvent(new TwiceUpvoteCancelledEvent<>(
                    user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO
                ));
            } else {
                eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                    user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, postDO.getNodeId()
                ));
            }
            return;
        }

        if (upvoteDO == null) {
            // 场景2：新增点赞

            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(user.getId());
            upvoteDO.setObjectId(postDO.getId());
            upvoteDO.setObjectType(ContentType.post.value());
            upvoteDO.setType(type);
            upvoteDataService.insert(upvoteDO);

            // 发布点赞事件
            if (type == Enums.VoteType.twice.value()) {
                eventPublisher.publishEvent(new TwiceUpvotedEvent<>(
                    user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO
                ));
            } else {
                eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                    user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, postDO.getNodeId()
                ));
            }
        } else {
            // 场景3：切换点赞类型
            int oldType = upvoteDO.getType();

            // 更新点赞记录类型
            upvoteDO.setType(type);
            upvoteDataService.update(upvoteDO);

            // 发布点赞类型切换事件（一次性处理，避免重复计算）
            eventPublisher.publishEvent(new UpvoteTypeSwitchedEvent<>(
                user.getId(), postDO.getId(), ContentType.post, postDO.getCreatorId(), postDO, oldType, type
            ));
        }
    }

    /**
     * 获取评论对应的节点ID
     * 
     * @param commentDO 评论对象
     * @return 节点ID
     * @throws BusinessException 当评论类型无效时抛出异常
     */
    private long getNodeIdFromComment(CommentDO commentDO) {
        if (commentDO.getObjectType() == ContentType.node.value()) {
            return commentDO.getObjectId();
        } else if (commentDO.getObjectType() == ContentType.post.value()) {
            PostDO postDO = postDataService.validateAndGet(commentDO.getObjectId());
            return postDO.getNodeId();
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的评论类型: " + commentDO.getObjectType());
        }
    }

    /**
     * 评论点赞
     *
     * 支持评论的点赞和取消点赞
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param commentDO 评论对象（已验证存在）
     * @param user 用户对象
     * @throws BusinessException 当参数无效时抛出异常
     */
    @Transactional
    public void upvoteComment(CommentDO commentDO, UserDO user) {
        // 查询现有点赞记录
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(user.getId(), commentDO.getId(), ContentType.comment.value());

        if (upvoteDO != null) {
            // 场景1：取消点赞
            upvoteDataService.delete(upvoteDO.getId());

            // 发布取消点赞事件（评论只有一种点赞类型，使用 LikeUpvoteCancelledEvent）
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), commentDO.getId(), ContentType.comment,
                commentDO.getCreatorId(), commentDO, getNodeIdFromComment(commentDO)
            ));
        } else {
            // 场景2：新增点赞
            upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(user.getId());
            upvoteDO.setObjectId(commentDO.getId());
            upvoteDO.setObjectType(ContentType.comment.value());
            upvoteDO.setType(0); // 评论点赞类型设为0
            upvoteDataService.insert(upvoteDO);

            // 发布点赞事件（评论只有一种点赞类型，使用 LikeUpvotedEvent）
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), commentDO.getId(), ContentType.comment,
                commentDO.getCreatorId(), commentDO, getNodeIdFromComment(commentDO)
            ));
        }
    }

    /**
     * 路线图投票
     *
     * 支持路线图的投票和取消投票
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param roadmapDO 路线图对象（已验证存在）
     * @param user 用户对象
     * @return true表示投票成功，false表示取消投票
     * @throws BusinessException 当参数无效时抛出异常
     */
    @Transactional
    public boolean upvoteRoadmap(RoadmapDO roadmapDO, UserDO user) {
        // 检查是否已经投过票
        UpvoteDO existingUpvote = upvoteDataService.getByUserAndObject(user.getId(), roadmapDO.getId(), ContentType.roadmap.value());

        if (existingUpvote != null) {
            // 如果已经投过票，则取消投票
            upvoteDataService.delete(existingUpvote.getId());

            // 发布取消投票事件
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), roadmapDO.getId(), ContentType.roadmap,
                roadmapDO.getCreatorId(), roadmapDO, roadmapDO.getProfessionId()
            ));

            return false; // 返回false表示取消投票
        } else {
            // 如果没有投过票，则投票
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(user.getId());
            upvoteDO.setObjectId(roadmapDO.getId());
            upvoteDO.setObjectType(ContentType.roadmap.value());
            upvoteDO.setType(0); // roadmap投票类型设为0，对应"once"
            upvoteDataService.insert(upvoteDO);

            // 发布投票事件
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), roadmapDO.getId(), ContentType.roadmap,
                roadmapDO.getCreatorId(), roadmapDO, roadmapDO.getProfessionId()
            ));

            return true; // 返回true表示投票成功
        }
    }

    /**
     * 记忆卡片组点赞
     *
     * 支持记忆卡片组的点赞和取消点赞
     * 使用事件驱动架构，通过发布事件来处理统计更新、消息通知等副作用操作
     *
     * @param deck 卡片组对象（已验证存在）
     * @param user 用户对象
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean upvoteMemoryCardDeck(MemoryCardDeckDO deck, UserDO user) {
        // 检查是否已经点过赞
        UpvoteDO existingUpvote = upvoteDataService.getByUserAndObject(user.getId(), deck.getId(), ContentType.memory_card_deck.value());
        if (existingUpvote != null) {
            // 如果已经点过赞，则取消点赞
            upvoteDataService.delete(existingUpvote.getId());

            // 发布取消点赞事件
            eventPublisher.publishEvent(new LikeUpvoteCancelledEvent<>(
                user.getId(), deck.getId(), ContentType.memory_card_deck,
                deck.getCreatorId(), deck, deck.getNodeId()
            ));

            return false; // 返回false表示取消点赞
        } else {
            // 如果没有点过赞，则点赞
            UpvoteDO upvoteDO = new UpvoteDO();
            upvoteDO.setUserId(user.getId());
            upvoteDO.setObjectId(deck.getId());
            upvoteDO.setObjectType(ContentType.memory_card_deck.value());
            upvoteDO.setType(0); // 卡片组点赞类型设为0
            upvoteDataService.insert(upvoteDO);

            // 发布点赞事件
            eventPublisher.publishEvent(new LikeUpvotedEvent<>(
                user.getId(), deck.getId(), ContentType.memory_card_deck,
                deck.getCreatorId(), deck, deck.getNodeId()
            ));

            return true; // 返回true表示点赞成功
        }
    }

    /**
     * 检查用户是否已经给指定内容投过票
     * @param contentId 内容ID
     * @param contentType 内容类型
     * @param userId 用户ID
     * @return true表示已投票，false表示未投票
     */
    public boolean hasUpvoted(long contentId, ContentType contentType, long userId) {
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }
        if (contentId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("内容ID无效: " + contentId);
        }

        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, contentId, contentType.value());
        return upvoteDO != null;
    }

    /**
     * 批量检查用户对指定内容的投票状态
     * @param contentIds 内容ID列表
     * @param contentType 内容类型
     * @param userId 用户ID
     * @return 已投票的内容ID集合
     */
    public Set<Long> getUpvotedIds(List<Long> contentIds, ContentType contentType, long userId) {
        if (contentIds == null || contentIds.isEmpty() || userId <= 0) {
            return new HashSet<>();
        }

        List<UpvoteDO> upvotes = upvoteDataService.getList(userId, contentIds, contentType.value());
        return upvotes.stream()
                .map(UpvoteDO::getObjectId)
                .collect(Collectors.toSet());
    }

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
            throw ErrorCode.INVALID_PARAMETER.exception("对象ID无效: " + objectId);
        }
        if (userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception("用户ID无效: " + userId);
        }

        // 查询用户点赞记录
        UpvoteDO upvoteDO = upvoteDataService.getByUserAndObject(userId, objectId, contentType.value());

        // 构建用户点赞状态DTO
        boolean twiceUpvoted = false;
        boolean likeUpvoted = false;

        if (upvoteDO != null) {
            // 帖子特殊处理：支持 twice 和 like 分别统计
            if (contentType == ContentType.post) {
                twiceUpvoted = upvoteDO.getType() == Enums.VoteType.twice.value();
                likeUpvoted = upvoteDO.getType() == Enums.VoteType.like.value();
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

}
