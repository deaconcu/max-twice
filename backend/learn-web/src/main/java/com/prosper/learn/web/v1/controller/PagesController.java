package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.service.PageService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 页面聚合接口
 * 从AggregateClient拆分出的页面聚合功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class PagesController {

    private final PageService pageService;

    /**
     * 根据不同参数读取页面数据
     * 映射: GET /read?courseId=123&path=xxx → GET /api/v1/pages/read?courseId=123&path=xxx
     * 映射: GET /read?nodeId=123 → GET /api/v1/pages/read?nodeId=123
     * 映射: GET /read?postId=123 → GET /api/v1/pages/read?postId=123
     * 映射: GET /read?commentId=123 → GET /api/v1/pages/read?commentId=123
     */
    @GetMapping("/pages/read")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Map<String, Object>> read(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "评论ID必须大于0") Long commentId,
            @CurrentUser UserDO currentUser) {

        Map<String, Object> result;

        // 参数优先级是 commentId > postId > nodeId > courseId + path
        if (commentId != null) {
            result = pageService.readPageByComment(commentId, currentUser.getId());
        } else if (postId != null) {
            result = pageService.readPageByPost(postId, currentUser.getId());
        } else if (nodeId != null) {
            // nodeId 可以配合 path 使用，也可以单独使用
            result = pageService.readPageByNode(nodeId, path, currentUser.getId());
        } else if (courseId != null) {
            // Course 是特殊的 Root Node，转换 courseId 为 rootNodeId 后使用统一的 Node 访问方式
            result = pageService.readPageByCourse(courseId, path, currentUser.getId());
        } else {
            throw StatusCode.INVALID_PARAMETER.exception();
        }

        return ApiResponse.success(result);
    }

    /**
     * 读取节点帖子列表（优化版本）
     * 仅返回 NodePostsPage 需要的数据：node, course, parentCourse, subCourseList, otherPostings, users, learning
     * 不返回 TOC、fixedPostings、chosenPosting，节省带宽和查询
     */
    @GetMapping("/pages/node")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Map<String, Object>> readNode(
            @RequestParam @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @CurrentUser UserDO currentUser) {
        // 使用统一的 readPageByNode 方法，path 为 null 表示访问根节点
        Map<String, Object> result = pageService.readPageByNode(nodeId, null, currentUser.getId());
        return ApiResponse.success(result);
    }

    /**
     * 读取帖子详情（优化版本）
     * 仅返回 PostDetailPage 需要的数据：node, course, parentCourse, subCourseList, post, users, learning
     * 不返回 TOC、fixedPostings、otherPostings，节省带宽和查询
     * 支持通过 postId 或 commentId 定位
     */
    @GetMapping("/pages/post")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Map<String, Object>> readPost(
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "评论ID必须大于0") Long commentId,
            @CurrentUser UserDO currentUser) {

        if (postId == null && commentId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("必须提供 postId 或 commentId");
        }

        Map<String, Object> result;
        if (commentId != null) {
            result = pageService.readPageForComment(commentId, currentUser.getId());
        } else {
            result = pageService.readPageForPost(postId, currentUser.getId());
        }

        return ApiResponse.success(result);
    }
}