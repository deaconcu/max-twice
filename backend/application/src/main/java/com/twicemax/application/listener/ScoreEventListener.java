package com.twicemax.application.listener;

import com.twicemax.application.service.ScoreCalculationService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.post.PostDataService;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapDataService;
import com.twicemax.interaction.comment.CommentDO;
import com.twicemax.interaction.comment.CommentDataService;
import com.twicemax.memory.deck.MemoryCardDeckDO;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.event.content.lifecycle.CommentCreatedEvent;
import com.twicemax.shared.domain.event.content.voting.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 分数计算事件监听器
 *
 * 专门负责处理需要重新计算分数的事件：
 * - 帖子点赞相关事件（影响帖子分数）
 * - 评论相关事件（可能影响帖子分数）
 *
 * 注意：只有帖子需要分数计算，其他内容类型暂不支持
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreEventListener {

    private final ScoreCalculationService scoreCalculationService;
    private final PostDataService postDataService;
    private final RoadmapDataService roadmapDataService;
    private final CommentDataService commentDataService;

    // ==================== 帖子点赞事件 ====================

    /**
     * 帖子两次能懂点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onPostTwiceUpvoted(TwiceUpvotedEvent<PostDO> event) {
        if (event.getContentType() == ContentType.post) {
            try {
                scoreCalculationService.checkAndUpdatePostScore(event.getContentObject());
                log.debug("重新计算帖子分数: postId={}", event.getContentId());
            } catch (Exception e) {
                log.error("帖子两次能懂点赞分数计算失败: postId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 帖子点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onPostLikeUpvoted(LikeUpvotedEvent<PostDO> event) {
        if (event.getContentType() == ContentType.post) {
            try {
                scoreCalculationService.checkAndUpdatePostScore(event.getContentObject());
                log.debug("重新计算帖子分数: postId={}", event.getContentId());
            } catch (Exception e) {
                log.error("帖子点赞分数计算失败: postId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 取消帖子两次能懂点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onPostTwiceUpvoteCancelled(TwiceUpvoteCancelledEvent<PostDO> event) {
        if (event.getContentType() == ContentType.post) {
            try {
                scoreCalculationService.checkAndUpdatePostScore(event.getContentObject());
                log.debug("重新计算帖子分数（取消两次能懂）: postId={}", event.getContentId());
            } catch (Exception e) {
                log.error("取消帖子两次能懂点赞分数计算失败: postId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 取消帖子点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onPostLikeUpvoteCancelled(LikeUpvoteCancelledEvent<PostDO> event) {
        if (event.getContentType() == ContentType.post) {
            try {
                scoreCalculationService.checkAndUpdatePostScore(event.getContentObject());
                log.debug("重新计算帖子分数（取消点赞）: postId={}", event.getContentId());
            } catch (Exception e) {
                log.error("取消帖子点赞分数计算失败: postId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 点赞类型切换事件 - 重新计算分数（只计算一次）
     */
    @EventListener
    //@Async
    public void onUpvoteTypeSwitched(UpvoteTypeSwitchedEvent<PostDO> event) {
        if (event.getContentType() == ContentType.post) {
            try {
                scoreCalculationService.checkAndUpdatePostScore(event.getContentObject());
                log.debug("重新计算帖子分数（切换点赞类型）: postId={}, from={}, to={}",
                    event.getContentId(), event.getFromType(), event.getToType());
            } catch (Exception e) {
                log.error("点赞类型切换分数计算失败: postId={}", event.getContentId(), e);
            }
        }
    }

    // ==================== 路线图点赞事件 ====================

    /**
     * 路线图点赞 - 重新计算分数
     */
    @EventListener
    public void onRoadmapLikeUpvoted(LikeUpvotedEvent<RoadmapDO> event) {
        if (event.getContentType() == ContentType.roadmap) {
            try {
                scoreCalculationService.checkAndUpdateRoadmapScore(event.getContentObject());
                log.debug("重新计算路线图分数: roadmapId={}", event.getContentId());
            } catch (Exception e) {
                log.error("路线图点赞分数计算失败: roadmapId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 取消路线图点赞 - 重新计算分数
     */
    @EventListener
    public void onRoadmapLikeUpvoteCancelled(LikeUpvoteCancelledEvent<RoadmapDO> event) {
        if (event.getContentType() == ContentType.roadmap) {
            try {
                scoreCalculationService.checkAndUpdateRoadmapScore(event.getContentObject());
                log.debug("重新计算路线图分数（取消点赞）: roadmapId={}", event.getContentId());
            } catch (Exception e) {
                log.error("取消路线图点赞分数计算失败: roadmapId={}", event.getContentId(), e);
            }
        }
    }

    // ==================== 记忆卡片组点赞事件 ====================

    /**
     * 记忆卡片组点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onMemoryDeckLikeUpvoted(LikeUpvotedEvent<MemoryCardDeckDO> event) {
        if (event.getContentType() == ContentType.memory_card_deck) {
            try {
                scoreCalculationService.checkAndUpdateMemoryCardDeckScore(event.getContentObject());
                log.debug("重新计算记忆卡片组分数: deckId={}", event.getContentId());
            } catch (Exception e) {
                log.error("记忆卡片组点赞分数计算失败: deckId={}", event.getContentId(), e);
            }
        }
    }

    /**
     * 取消记忆卡片组点赞 - 重新计算分数
     */
    @EventListener
    //@Async
    public void onMemoryDeckLikeUpvoteCancelled(LikeUpvoteCancelledEvent<MemoryCardDeckDO> event) {
        if (event.getContentType() == ContentType.memory_card_deck) {
            try {
                scoreCalculationService.checkAndUpdateMemoryCardDeckScore(event.getContentObject());
                log.debug("重新计算记忆卡片组分数（取消点赞）: deckId={}", event.getContentId());
            } catch (Exception e) {
                log.error("取消记忆卡片组点赞分数计算失败: deckId={}", event.getContentId(), e);
            }
        }
    }

    // ==================== 其他可能影响分数的事件 ====================

    /**
     * 评论创建 - 可能影响帖子/父评论分数（评论数增加）
     */
    @EventListener
    //@Async
    public void onCommentCreated(CommentCreatedEvent event) {
        try {
            // 如果评论的是帖子，重新计算帖子分数
            if (event.getContentType() == ContentType.post) {
                PostDO postDO = postDataService.getById(event.getContentId());
                if (postDO != null) {
                    scoreCalculationService.checkAndUpdatePostScore(postDO);
                    log.debug("重新计算帖子分数（新评论）: postId={}", event.getContentId());
                }
            }
            // 如果评论的是评论（回复），重新计算父评论分数
            else if (event.getContentType() == ContentType.comment) {
                CommentDO commentDO = commentDataService.getById(event.getContentId());
                if (commentDO != null) {
                    scoreCalculationService.checkAndUpdateCommentScore(commentDO);
                    log.debug("重新计算评论分数（新回复）: commentId={}", event.getContentId());
                }
            }
        } catch (Exception e) {
            log.error("评论创建分数计算失败: contentType={}, contentId={}",
                event.getContentType(), event.getContentId(), e);
        }
    }
}