package com.prosper.learn.web.v1.controller;

import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.web.v1.annotation.RequireLogin;
import com.prosper.learn.common.Enums.DailyStatType;
import com.prosper.learn.common.Enums.CumulativeStatType;
import com.prosper.learn.business.service.UserStatsService;
import com.prosper.learn.dto.response.UserStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户统计API接口
 * 基于新的用户统计系统
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsService userStatsService;

    /**
     * 获取用户当前统计
     */
    @GetMapping("/{userId}/stats")
    public ApiResponse<UserStatsDTO> getUserStats(@PathVariable Long userId) {
        UserStatsDTO stats = userStatsService.getCurrentUserStats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 批量获取用户统计（排行榜用）
     */
    @PostMapping("/stats/batch")
    public ApiResponse<Map<Long, UserStatsDTO>> batchGetUserStats(@RequestBody List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return ApiResponse.success(Map.of());
        }

        // 限制批量查询数量
        if (userIds.size() > 100) {
            userIds = userIds.subList(0, 100);
        }

        Map<Long, UserStatsDTO> stats = userStatsService.batchGetUserStats(userIds);
        return ApiResponse.success(stats);
    }

    /**
     * 手动触发数据同步（管理功能）
     */
    @PostMapping("/{userId}/stats/refresh")
    @RequireLogin
    public ApiResponse<UserStatsDTO> refreshUserStats(@PathVariable Long userId) {
        // TODO: 添加权限检查，确保只有管理员或用户本人可以刷新
        userStatsService.forceRefreshUserStats(userId);
        UserStatsDTO stats = userStatsService.getCurrentUserStats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 内部API：增量更新日度统计
     * 注意：这个接口应该只供内部服务调用，不对外暴露
     */
    @PostMapping("/{userId}/stats/daily/increment")
    @RequireLogin // TODO: 改为内部服务认证
    public ApiResponse<Void> incrementDailyStat(
            @PathVariable Long userId,
            @RequestParam String statType,
            @RequestParam(defaultValue = "1") int delta) {

        try {
            DailyStatType type = DailyStatType.valueOf(statType.toUpperCase());
            userStatsService.incrementDailyStat(userId, type, delta);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid daily stat type: {}", statType);
            return ApiResponse.error(400, "无效的统计类型");
        }
    }

    /**
     * 内部API：增量更新累计统计
     * 注意：这个接口应该只供内部服务调用，不对外暴露
     */
    @PostMapping("/{userId}/stats/cumulative/increment")
    @RequireLogin // TODO: 改为内部服务认证
    public ApiResponse<Void> incrementCumulativeStat(
            @PathVariable Long userId,
            @RequestParam String statType,
            @RequestParam(defaultValue = "1") int delta) {

        try {
            CumulativeStatType type = CumulativeStatType.valueOf(statType.toUpperCase());
            userStatsService.incrementCumulativeStat(userId, type, delta);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cumulative stat type: {}", statType);
            return ApiResponse.error(400, "无效的统计类型");
        }
    }

    /**
     * 内部API：设置累计统计绝对值（数据修复用）
     * 注意：这个接口应该只供管理员使用
     */
    @PostMapping("/{userId}/stats/cumulative/set")
    @RequireLogin // TODO: 改为管理员权限检查
    public ApiResponse<Void> setCumulativeStat(
            @PathVariable Long userId,
            @RequestParam String statType,
            @RequestParam int newValue) {

        try {
            CumulativeStatType type = CumulativeStatType.valueOf(statType.toUpperCase());
            userStatsService.setCumulativeStat(userId, type, newValue);
            return ApiResponse.success();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cumulative stat type: {}", statType);
            return ApiResponse.error(400, "无效的统计类型");
        }
    }

    /**
     * 获取统计类型列表（前端用）
     */
    @GetMapping("/stats/types")
    public ApiResponse<Map<String, Object>> getStatTypes() {
        Map<String, Object> result = Map.of(
            "dailyTypes", Map.of(
                "VIEWS", Map.of("field", "daily_views", "description", "当日浏览量"),
                "TWICE", Map.of("field", "daily_twice", "description", "当日两次能懂点赞数"),
                "HELPFUL", Map.of("field", "daily_helpful", "description", "当日有帮助点赞数"),
                "COMMENTS", Map.of("field", "daily_comments", "description", "当日评论数")
            ),
            "cumulativeTypes", Map.of(
                "LEARNING_COURSES", Map.of("field", "learning_courses", "description", "正在学习课程数"),
                "COMPLETED_COURSES", Map.of("field", "completed_courses", "description", "已完成课程数"),
                "IN_PROGRESS_PROFESSIONS", Map.of("field", "in_progress_professions", "description", "正在进行职业数"),
                "COMPLETED_PROFESSIONS", Map.of("field", "completed_professions", "description", "已完成职业数"),
                "FOLLOWING_USERS", Map.of("field", "following_users", "description", "关注的人数"),
                "FOLLOWING_COURSES", Map.of("field", "following_courses", "description", "关注的课程数"),
                "FOLLOWING_PROFESSIONS", Map.of("field", "following_professions", "description", "关注的职业数"),
                "CREATED_ARTICLES", Map.of("field", "created_articles", "description", "创建的文章数"),
                "CREATED_INDEXS", Map.of("field", "created_indexs", "description", "创建的目录数"),
                "CREATED_ROADMAPS", Map.of("field", "created_roadmaps", "description", "创建的路线图数"),
                "CREATED_CARD_DECKS", Map.of("field", "created_card_decks", "description", "创建的卡片组数")
            )
        );
        return ApiResponse.success(result);
    }
}