package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.PostContentsRequest;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import java.util.concurrent.TimeUnit;

/**
 * 内容管理接口
 * 从AggregateClient拆分出的内容管理功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 80, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class ContentsController {

    private final TocDomainService tocService;

    /**
     * 内容操作（选择、固定等）
     * 映射: POST /contents → POST /api/v1/contents
     */
    @PostMapping("/contents")
    @SaCheckLogin
    public ApiResponse<Void> postContents(
            @RequestBody @Valid PostContentsRequest request,
            Model model,
            @CurrentUser UserDO currentUser) {

        switch (request.getAction()) {
            case 1:
                tocService.choose(currentUser.getId(), request.getPath(), request.getCourseId(), request.getPostingId());
                break;
            case 2:
                tocService.unchoose(currentUser.getId(), request.getCourseId(), request.getPath());
                break;
            case 3:
                tocService.pin(currentUser.getId(), request.getCourseId(), request.getPath(), request.getPostingId(), true);
                break;
            case 4:
                tocService.pin(currentUser.getId(), request.getCourseId(), request.getPath(), request.getPostingId(), false);
                break;
            default:
                throw ErrorCode.NOT_SUPPORTED.exception();
        }
        return ApiResponse.success();
    }
}