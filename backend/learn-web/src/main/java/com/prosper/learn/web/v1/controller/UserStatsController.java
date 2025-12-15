package com.prosper.learn.web.v1.controller;

import com.prosper.learn.analytics.stats.service.UserStatsDomainService;
import com.prosper.learn.analytics.dto.UserStatsDTO;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.web.v1.annotation.RequireLogin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户统计API接口
 *
 * 提供用户累计统计数据的查询和更新接口
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserStatsController {

    private final UserStatsDomainService userStatsService;

    /**
     * 获取用户统计数据
     */
    @GetMapping("/{userId}/stats")
    public ApiResponse<UserStatsDTO> getUserStats(@PathVariable Long userId) {
        UserStatsDTO stats = userStatsService.getUserStats(userId);
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
     * 获取排行榜 Top N 用户
     */
    @GetMapping("/stats/top")
    public ApiResponse<List<UserStatsDTO>> getTopUsers(
            @RequestParam(defaultValue = "views") String field,
            @RequestParam(defaultValue = "10") int limit) {

        // 限制最大查询数量
        if (limit > 100) {
            limit = 100;
        }

        List<UserStatsDTO> topUsers = userStatsService.getTopUsersByField(field, limit);
        return ApiResponse.success(topUsers);
    }


    /**
     * 内部API：增量更新浏览量
     *
     * @deprecated 此接口不应该对外暴露。统计更新已通过事件驱动自动完成：
     *   - UserStatsEventListener 监听各种业务事件
     *   - 自动调用 UserStatsDomainService 的 increment 方法
     *   - 前端未使用此接口
     *   - 对外暴露有安全风险：用户可随意刷统计数据
     *
     * 如需手动修复数据，应使用管理后台工具或数据库脚本，而非HTTP接口。
     */
//    @PostMapping("/{userId}/stats/views/increment")
//    @RequireLogin
//    public ApiResponse<Void> incrementViews(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "1") int delta) {
//
//        userStatsService.incrementViews(userId, delta);
//        return ApiResponse.success();
//    }

    /**
     * 内部API：增量更新两次能懂数
     *
     * @deprecated 此接口不应该对外暴露。统计更新已通过事件驱动自动完成。
     *             详见 incrementViews 的注释说明。
     */
//    @PostMapping("/{userId}/stats/twices/increment")
//    @RequireLogin
//    public ApiResponse<Void> incrementTwices(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "1") int delta) {
//
//        userStatsService.incrementTwices(userId, delta);
//        return ApiResponse.success();
//    }

    /**
     * 内部API：增量更新有用点赞数
     *
     * @deprecated 此接口不应该对外暴露。统计更新已通过事件驱动自动完成。
     *             详见 incrementViews 的注释说明。
     */
//    @PostMapping("/{userId}/stats/likes/increment")
//    @RequireLogin
//    public ApiResponse<Void> incrementLikes(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "1") int delta) {
//
//        userStatsService.incrementLikes(userId, delta);
//        return ApiResponse.success();
//    }

    /**
     * 内部API：增量更新评论数
     *
     * @deprecated 此接口不应该对外暴露。统计更新已通过事件驱动自动完成。
     *             详见 incrementViews 的注释说明。
     */
//    @PostMapping("/{userId}/stats/comments/increment")
//    @RequireLogin
//    public ApiResponse<Void> incrementComments(
//            @PathVariable Long userId,
//            @RequestParam(defaultValue = "1") int delta) {
//
//        userStatsService.incrementComments(userId, delta);
//        return ApiResponse.success();
//    }

    /**
     * 内部API：设置统计字段绝对值（数据修复用）
     *
     * @deprecated 此接口不应该对外暴露。数据修复应该通过：
     *   - 管理后台专用工具
     *   - 数据库脚本
     *   - 定时任务重新计算
     *   而不是暴露为HTTP接口，避免被滥用。
     */
//    @PostMapping("/{userId}/stats/set")
//    @RequireLogin
//    public ApiResponse<Void> setStatField(
//            @PathVariable Long userId,
//            @RequestParam String field,
//            @RequestParam int newValue) {
//
//        // 验证字段名
//        List<String> validFields = List.of(
//            "views", "twices", "likes", "comments",
//            "learning_courses", "completed_courses",
//            "in_progress_professions", "completed_professions",
//            "following_users", "following_courses", "following_professions",
//            "created_articles", "created_indexs", "created_roadmaps", "created_card_decks"
//        );
//
//        if (!validFields.contains(field)) {
//            return ApiResponse.error(400, "无效的字段名");
//        }
//
//        userStatsService.setField(userId, field, newValue);
//        return ApiResponse.success();
//    }

    /**
     * 获取统计字段列表（前端用）
     */
    @GetMapping("/stats/fields")
    public ApiResponse<Map<String, Object>> getStatFields() {
        Map<String, Object> result = Map.of(
            "dailyStats", Map.of(
                "views", "总浏览量",
                "twices", "总两次能懂点赞数",
                "likes", "总有用点赞数",
                "comments", "总评论数"
            ),
            "learningStats", Map.of(
                "learning_courses", "正在学习课程数",
                "completed_courses", "已完成课程数",
                "in_progress_professions", "正在进行职业数",
                "completed_professions", "已完成职业数"
            ),
            "socialStats", Map.of(
                "following_users", "关注的人数",
                "following_courses", "关注的课程数",
                "following_professions", "关注的职业数"
            ),
            "creationStats", Map.of(
                "created_articles", "创建的文章数",
                "created_indexs", "创建的目录数",
                "created_roadmaps", "创建的路线图数",
                "created_card_decks", "创建的卡片组数"
            )
        );
        return ApiResponse.success(result);
    }
}
