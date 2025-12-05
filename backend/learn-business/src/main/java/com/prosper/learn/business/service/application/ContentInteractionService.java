package com.prosper.learn.business.service.application;

import com.prosper.learn.business.service.domain.UpvoteDomainService;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.MemoryCardDeckDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 内容交互应用服务
 *
 * 负责编排复杂的用户与内容交互用例，包括：
 * - 帖子点赞（需要验证帖子存在性和点赞类型）
 * - 评论点赞（需要验证评论存在性）
 * - 路线图投票（需要验证路线图存在性）
 * - 记忆卡片组点赞（需要验证卡片组存在性）
 *
 * 职责：
 * 1. 验证和获取领域对象
 * 2. 调用领域服务执行业务逻辑
 * 3. 管理事务边界
 *
 * 注意：简单的查询方法（如hasUpvoted、getUpvotedIds等）
 * 应该直接在Controller中调用对应的DomainService，无需经过此服务
 *
 * @author Claude
 * @since 2024-12-03
 */
@Service
@RequiredArgsConstructor
public class ContentInteractionService {

    /** 点赞领域服务 */
    private final UpvoteDomainService upvoteDomainService;

    /** 帖子数据访问接口 */
    private final PostDataService postDataService;

    /** 评论数据访问接口 */
    private final CommentDataService commentDataService;

    /** 路线图数据访问接口 */
    private final RoadmapDataService roadmapDataService;

    /** 记忆卡片组数据访问接口 */
    private final MemoryCardDeckDataService deckDataService;

    /**
     * 帖子点赞
     *
     * 支持多种点赞类型：twice, like
     * 包含点赞、取消点赞、切换点赞类型的完整逻辑
     *
     * 编排流程：
     * 1. 验证帖子存在性
     * 2. 执行点赞业务逻辑（包含事件发布）
     *
     * @param postId 帖子ID
     * @param user 用户
     * @param type 点赞类型（1-twice, 2-like）
     * @throws com.prosper.learn.common.exception.BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvotePost(long postId, UserDO user, int type) {
        // 1. 验证和获取领域对象（应用层职责）
        PostDO postDO = postDataService.validateAndGet(postId);

        // 2. 调用领域服务执行业务逻辑（包含事件发布）
        upvoteDomainService.upvotePost(postDO, user, type);
    }

    /**
     * 评论点赞
     *
     * 支持评论的点赞和取消点赞
     *
     * 编排流程：
     * 1. 验证评论存在性
     * 2. 执行点赞业务逻辑（包含事件发布）
     *
     * @param commentId 评论ID
     * @param user 用户对象
     * @throws com.prosper.learn.common.exception.BusinessException 当参数无效或对象不存在时抛出异常
     */
    @Transactional
    public void upvoteComment(long commentId, UserDO user) {
        // 1. 验证和获取领域对象（应用层职责）
        CommentDO commentDO = commentDataService.validateAndGet(commentId);

        // 2. 调用领域服务执行业务逻辑（包含事件发布）
        upvoteDomainService.upvoteComment(commentDO, user);
    }

    /**
     * 路线图投票
     *
     * 支持路线图的投票和取消投票
     *
     * 编排流程：
     * 1. 验证路线图存在性
     * 2. 执行投票业务逻辑（包含事件发布）
     *
     * @param roadmapId 路线图ID
     * @param user 用户对象
     * @return true表示投票成功，false表示取消投票
     * @throws com.prosper.learn.common.exception.BusinessException 当参数无效时抛出异常
     */
    @Transactional
    public boolean upvoteRoadmap(long roadmapId, UserDO user) {
        // 1. 验证和获取领域对象（应用层职责）
        RoadmapDO roadmapDO = roadmapDataService.validateAndGet(roadmapId);

        // 2. 调用领域服务执行业务逻辑（包含事件发布）
        return upvoteDomainService.upvoteRoadmap(roadmapDO, user);
    }

    /**
     * 记忆卡片组点赞
     *
     * 支持记忆卡片组的点赞和取消点赞
     *
     * 编排流程：
     * 1. 验证卡片组存在性
     * 2. 执行点赞业务逻辑（包含事件发布）
     *
     * @param deckId 卡片组ID
     * @param user 用户对象
     * @return true表示点赞成功，false表示取消点赞
     */
    @Transactional
    public boolean upvoteMemoryCardDeck(long deckId, UserDO user) {
        // 1. 验证和获取领域对象（应用层职责）
        MemoryCardDeckDO deck = deckDataService.validateAndGet(deckId);

        // 2. 调用领域服务执行业务逻辑（包含事件发布）
        return upvoteDomainService.upvoteMemoryCardDeck(deck, user);
    }
}