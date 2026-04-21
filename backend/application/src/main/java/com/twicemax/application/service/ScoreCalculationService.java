package com.twicemax.application.service;

import com.twicemax.analytics.scoring.ScoreCalculationDomainService;
import com.twicemax.content.post.PostDO;
import com.twicemax.content.post.PostMapper;
import com.twicemax.content.roadmap.RoadmapDO;
import com.twicemax.content.roadmap.RoadmapMapper;
import com.twicemax.interaction.comment.CommentDO;
import com.twicemax.interaction.comment.CommentMapper;
import com.twicemax.memory.deck.MemoryCardDeckDO;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 评分计算应用服务
 * 负责协调各领域内容的分数计算和更新
 *
 * 职责：
 * - 协调跨领域的分数计算（Post、Roadmap、Comment、MemoryCardDeck）
 * - 调用 DomainService 执行核心算法
 * - 更新各领域的分数到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculationService {

    // 领域服务
    private final ScoreCalculationDomainService domainService;

    // Mapper（用于更新分数）
    private final PostMapper postMapper;
    private final RoadmapMapper roadmapMapper;
    private final CommentMapper commentMapper;

    // ========== 公共方法 ==========

    /**
     * 检查并更新文章分数（如果需要的话）
     */
    public boolean checkAndUpdatePostScore(PostDO post) {
        if (post == null || post.getId() <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("帖子对象无效");
        }

        try {
            if (domainService.shouldUpdateScore(post.getScoreCalculatedAt())) {
                double score = domainService.calculateContentScore(post.getId(), Enums.ContentType.post);
                postMapper.updateScore(post.getId(), score);
                log.debug("实时更新文章分数: postId={}, score={}", post.getId(), score);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新文章分数失败: postId={}", post.getId(), e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 检查并更新路线图分数（如果需要的话）
     */
    public boolean checkAndUpdateRoadmapScore(RoadmapDO roadmap) {
        if (roadmap == null || roadmap.getId() <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("路线图对象无效");
        }

        try {
            if (domainService.shouldUpdateScore(roadmap.getScoreCalculatedAt())) {
                double score = domainService.calculateContentScore(roadmap.getId(), Enums.ContentType.roadmap);
                roadmapMapper.updateScore(roadmap.getId(), score);
                log.debug("实时更新路线图分数: roadmapId={}, score={}", roadmap.getId(), score);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新路线图分数失败: roadmapId={}", roadmap.getId(), e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 检查并更新评论分数（如果需要的话）
     */
    public boolean checkAndUpdateCommentScore(CommentDO comment) {
        if (comment == null || comment.getId() <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("评论对象无效");
        }

        try {
            double score = domainService.calculateCommentScore(comment.getId(), comment.getCreatedAt());

            if (Math.abs(comment.getScore() - score) > 0.001) {
                comment.setScore(score);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新评论分数失败: commentId={}", comment.getId(), e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }

    /**
     * 检查并更新记忆卡片组分数（如果需要的话）
     */
    public boolean checkAndUpdateMemoryCardDeckScore(MemoryCardDeckDO deck) {
        if (deck == null || deck.getId() <= 0) {
            throw StatusCode.INVALID_PARAMETER.exception("卡片组对象无效");
        }

        try {
            double newScore = domainService.calculateMemoryCardDeckScore(deck.getId(), deck.getCreatedAt(), deck.getCardCount());

            if (deck.getScore() == null || Math.abs(deck.getScore() - newScore) > 0.001) {
                deck.setScore(newScore);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查并更新卡片组分数失败: deckId={}", deck.getId(), e);
            throw StatusCode.SYSTEM_ERROR.exception(e);
        }
    }
}
