package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.shared.domain.event.content.lifecycle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.ContentState;

/**
 * 用户统计领域服务
 *
 * 负责用户统计相关的业务逻辑，包括：
 * - 用户累计统计数据查询
 * - 统计数据增量更新
 * - 排行榜查询
 * - 监听内容审核事件，更新用户维度的统计（user_stats表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsDomainService {

    private final UserStatsDataService userStatsDataService;

    /**
     * 获取用户统计数据
     */
    public UserStatsDTO getUserStats(Long userId) {
        UserStatsDO stats = userStatsDataService.getOrCreate(userId);
        return convertToDTO(stats);
    }

    /**
     * 增量更新浏览量
     */
    @Transactional
    public void incrementViews(Long userId, int delta) {
        userStatsDataService.incrementViews(userId, delta);
    }

    /**
     * 增量更新两次能懂数
     */
    @Transactional
    public void incrementTwices(Long userId, int delta) {
        userStatsDataService.incrementTwices(userId, delta);
    }

    /**
     * 增量更新有用点赞数
     */
    @Transactional
    public void incrementLikes(Long userId, int delta) {
        userStatsDataService.incrementLikes(userId, delta);
    }

    /**
     * 增量更新评论数
     */
    @Transactional
    public void incrementComments(Long userId, int delta) {
        userStatsDataService.incrementComments(userId, delta);
    }

    /**
     * 设置字段绝对值（用于数据修复，内部使用，不暴露给外部）
     */
    @Transactional
    private void setField(Long userId, String field, int newValue) {
        // 根据field调用对应的专用方法
        switch (field) {
            case "following_users":
                userStatsDataService.setFollowingUsers(userId, newValue);
                break;
            case "following_courses":
                userStatsDataService.setFollowingCourses(userId, newValue);
                break;
            case "following_professions":
                userStatsDataService.setFollowingProfessions(userId, newValue);
                break;
            case "learning_courses":
                userStatsDataService.setLearningCourses(userId, newValue);
                break;
            case "completed_courses":
                userStatsDataService.setCompletedCourses(userId, newValue);
                break;
            case "in_progress_professions":
                userStatsDataService.setInProgressProfessions(userId, newValue);
                break;
            case "completed_professions":
                userStatsDataService.setCompletedProfessions(userId, newValue);
                break;
            default:
                log.warn("不支持的字段: {}", field);
                throw new IllegalArgumentException("不支持的字段: " + field);
        }
    }

    // ==================== 社交关系统计 ====================

    /**
     * 增量更新关注用户数
     */
    @Transactional
    public void incrementFollowingUsers(Long userId, int delta) {
        userStatsDataService.incrementFollowingUsers(userId, delta);
    }

    /**
     * 增量更新关注课程数
     */
    @Transactional
    public void incrementFollowingCourses(Long userId, int delta) {
        userStatsDataService.incrementFollowingCourses(userId, delta);
    }

    /**
     * 增量更新关注职业数
     */
    @Transactional
    public void incrementFollowingProfessions(Long userId, int delta) {
        userStatsDataService.incrementFollowingProfessions(userId, delta);
    }

    // ==================== 学习进度统计 ====================

    /**
     * 增量更新正在学习课程数
     */
    @Transactional
    public void incrementLearningCourses(Long userId, int delta) {
        userStatsDataService.incrementLearningCourses(userId, delta);
    }

    /**
     * 增量更新已完成课程数
     */
    @Transactional
    public void incrementCompletedCourses(Long userId, int delta) {
        userStatsDataService.incrementCompletedCourses(userId, delta);
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 增量更新正在进行职业数
//     */
//    @Transactional
//    public void incrementInProgressProfessions(Long userId, int delta) {
//        userStatsDataService.incrementInProgressProfessions(userId, delta);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 增量更新已完成职业数
//     */
//    @Transactional
//    public void incrementCompletedProfessions(Long userId, int delta) {
//        userStatsDataService.incrementCompletedProfessions(userId, delta);
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    // ==================== 内容创作统计 ====================

    /**
     * 增量更新创建文章数
     */
    @Transactional
    public void incrementCreatedArticles(Long userId, int delta) {
        userStatsDataService.incrementCreatedArticles(userId, delta);
    }

    /**
     * 增量更新创建目录数
     */
    @Transactional
    public void incrementCreatedIndexs(Long userId, int delta) {
        userStatsDataService.incrementCreatedIndexs(userId, delta);
    }

    /**
     * 增量更新创建路线图数
     */
    @Transactional
    public void incrementCreatedRoadmaps(Long userId, int delta) {
        userStatsDataService.incrementCreatedRoadmaps(userId, delta);
    }

    /**
     * 增量更新创建卡片组数
     */
    @Transactional
    public void incrementCreatedCardDecks(Long userId, int delta) {
        userStatsDataService.incrementCreatedCardDecks(userId, delta);
    }

    // ==================== 查询方法 ====================

    /**
     * 批量获取用户统计（排行榜用）
     */
    public Map<Long, UserStatsDTO> batchGetUserStats(List<Long> userIds) {
        Map<Long, UserStatsDO> statsMap = userStatsDataService.batchGetByUserIds(userIds);

        return statsMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToDTO(entry.getValue())
                ));
    }

    /**
     * 获取排行榜 Top N 用户
     */
    public List<UserStatsDTO> getTopUsersByField(String field, int limit) {
        List<UserStatsDO> topUsers = switch (field) {
            case "views" -> userStatsDataService.getTopUsersByViews(limit);
            case "twices" -> userStatsDataService.getTopUsersByTwices(limit);
            case "likes" -> userStatsDataService.getTopUsersByLikes(limit);
            case "comments" -> userStatsDataService.getTopUsersByComments(limit);
            case "created_articles" -> userStatsDataService.getTopUsersByCreatedArticles(limit);
            case "created_indexs" -> userStatsDataService.getTopUsersByCreatedIndexs(limit);
            case "created_roadmaps" -> userStatsDataService.getTopUsersByCreatedRoadmaps(limit);
            case "created_card_decks" -> userStatsDataService.getTopUsersByCreatedCardDecks(limit);
            default -> throw new IllegalArgumentException("不支持的排序字段: " + field);
        };
        return topUsers.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 转换为DTO
     */
    private UserStatsDTO convertToDTO(UserStatsDO statsDO) {
        if (statsDO == null) {
            return UserStatsDTO.empty();
        }

        return UserStatsDTO.builder()
                .userId(statsDO.getUserId())
                .viewCount(statsDO.getViewCount())
                .twiceCount(statsDO.getTwiceCount())
                .likeCount(statsDO.getLikeCount())
                .commentCount(statsDO.getCommentCount())
                .learningCourseCount(statsDO.getLearningCourseCount())
                .completedCourseCount(statsDO.getCompletedCourseCount())
                .inProgressProfessionCount(statsDO.getInProgressProfessionCount())
                .completedProfessionCount(statsDO.getCompletedProfessionCount())
                .followingUserCount(statsDO.getFollowingUserCount())
                .followingCourseCount(statsDO.getFollowingCourseCount())
                .followingProfessionCount(statsDO.getFollowingProfessionCount())
                .createdArticleCount(statsDO.getCreatedArticleCount())
                .createdIndexCount(statsDO.getCreatedIndexCount())
                .createdRoadmapCount(statsDO.getCreatedRoadmapCount())
                .createdCardDeckCount(statsDO.getCreatedCardDeckCount())
                .lastUpdated(statsDO.getUpdatedAt())
                .build();
    }

    // ==================== 事件监听：用户维度统计更新 ====================

    /**
     * 监听内容审核通过事件 - 增加用户创作统计
     */
    @EventListener
    //@Async
    public void onContentApproved(ContentApprovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostApproved(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), 1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), 1);
            }
        } catch (Exception e) {
            log.error("处理内容审核通过事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容下架事件 - 减少用户创作统计
     */
    @EventListener
    //@Async
    public void onContentRemoved(ContentRemovedEvent event) {
        try {
            switch (event.getContentType()) {
                case post -> handlePostRemoved(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), -1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), -1);
            }
        } catch (Exception e) {
            log.error("处理内容下架事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容封禁事件 - 减少用户创作统计（仅 PUBLISHED 状态）
     */
    @EventListener
    //@Async
    public void onContentBanned(ContentBannedEvent event) {
        try {
            if (event.getPreviousState() != ContentState.PUBLISHED.value()) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostBanned(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), -1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), -1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), -1);
            }
        } catch (Exception e) {
            log.error("处理内容封禁事件失败(用户统计): {}", e.getMessage());
        }
    }

    /**
     * 监听内容恢复事件 - 恢复用户创作统计
     */
    @EventListener
    //@Async
    public void onContentRestored(ContentRestoredEvent event) {
        try {
            if (event.getPreviousState() != ContentState.BANNED.value()) {
                return;
            }

            switch (event.getContentType()) {
                case post -> handlePostRestored(event);
                case roadmap -> userStatsDataService.incrementCreatedRoadmaps(event.getCreatorId(), 1);
                case memory_card_deck -> userStatsDataService.incrementCreatedCardDecks(event.getCreatorId(), 1);
                case comment -> userStatsDataService.incrementComments(event.getCreatorId(), 1);
            }
        } catch (Exception e) {
            log.error("处理内容恢复事件失败(用户统计): {}", e.getMessage());
        }
    }

    // ==================== Post 处理 ====================

    private void handlePostApproved(ContentApprovedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        }
    }

    private void handlePostRemoved(ContentRemovedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        }
    }

    private void handlePostBanned(ContentBannedEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), -1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), -1);
        }
    }

    private void handlePostRestored(ContentRestoredEvent event) {
        if (event.getPostType() == 1) {
            userStatsDataService.incrementCreatedArticles(event.getCreatorId(), 1);
        } else if (event.getPostType() == 2) {
            userStatsDataService.incrementCreatedIndexs(event.getCreatorId(), 1);
        }
    }
}