package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.service.PageService;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 页面聚合接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class PagesController {

    private final PageService pageService;

    @GetMapping("/pages/read")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Map<String, Object> read(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) @Positive(message = "节点ID必须大于0") Long nodeId,
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "评论ID必须大于0") Long commentId,
            @CurrentUser UserDO currentUser) {

        if (commentId != null) {
            return pageService.readPageByComment(commentId, currentUser.getId());
        } else if (postId != null) {
            return pageService.readPageByPost(postId, currentUser.getId());
        } else if (nodeId != null) {
            return pageService.readPageByNode(nodeId, path, currentUser.getId());
        } else if (courseId != null) {
            return pageService.readPageByCourse(courseId, path, currentUser.getId());
        } else {
            throw StatusCode.INVALID_PARAMETER.exception();
        }
    }

    @GetMapping("/pages/node")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Map<String, Object> readNode(
            @RequestParam @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @CurrentUser UserDO currentUser) {
        return pageService.readPageByNode(nodeId, null, currentUser.getId());
    }

    @GetMapping("/pages/post")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public Map<String, Object> readPost(
            @RequestParam(required = false) @Positive(message = "帖子ID必须大于0") Long postId,
            @RequestParam(required = false) @Positive(message = "评论ID必须大于0") Long commentId,
            @CurrentUser UserDO currentUser) {

        if (postId == null && commentId == null) {
            throw StatusCode.INVALID_PARAMETER.exception("必须提供 postId 或 commentId");
        }

        if (commentId != null) {
            return pageService.readPageForComment(commentId, currentUser.getId());
        } else {
            return pageService.readPageForPost(postId, currentUser.getId());
        }
    }
}
