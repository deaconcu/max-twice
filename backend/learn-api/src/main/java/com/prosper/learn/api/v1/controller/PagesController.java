package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.domain.service.business.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ApiResponse<Map<String, Object>> read(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "评论ID必须大于0") Long commentId) {

        long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result;

        // 参数优先级是 commentId > postId > nodeId > courseId + path
        if (commentId != null) {
            result = pageService.readPageByComment(commentId, userId);
        } else if (postId != null) {
            result = pageService.readPageByPost(postId, userId);
        } else if (nodeId != null) {
            result = pageService.readPageByNode(nodeId, userId);
        } else if (courseId != null) {
            result = pageService.readPageByPath(courseId, path, userId);
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }

        return ApiResponse.success(result);
    }
}