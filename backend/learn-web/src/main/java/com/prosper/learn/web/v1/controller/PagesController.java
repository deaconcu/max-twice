package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.business.service.application.PageService;
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
@RateLimit(capacity = 80, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
            result = pageService.readPageByNode(nodeId, currentUser.getId());
        } else if (courseId != null) {
            result = pageService.readPageByPath(courseId, path, currentUser.getId());
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }

        return ApiResponse.success(result);
    }
}