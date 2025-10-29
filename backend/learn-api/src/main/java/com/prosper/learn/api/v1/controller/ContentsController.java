package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.ContentsService;
import com.prosper.learn.dto.request.PostContentsRequest;
import com.prosper.learn.persistence.dataobject.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;

/**
 * 内容管理接口
 * 从AggregateClient拆分出的内容管理功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class ContentsController {

    private final ContentsService contentsService;

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
                contentsService.choose(currentUser.getId(), request.getPath(), request.getCourseId(), request.getPostingId());
                break;
            case 2:
                contentsService.unchoose(currentUser.getId(), request.getCourseId(), request.getPath());
                break;
            case 3:
                contentsService.pin(currentUser.getId(), request.getCourseId(), request.getPath(), request.getPostingId(), true);
                break;
            case 4:
                contentsService.pin(currentUser.getId(), request.getCourseId(), request.getPath(), request.getPostingId(), false);
                break;
            default:
                throw ErrorCode.NOT_SUPPORTED.exception();
        }
        return ApiResponse.success();
    }
}