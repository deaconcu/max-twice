package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.ContentsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 内容管理接口
 * 从AggregateClient拆分出的内容管理功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentsController {

    private final ContentsService contentsService;

    /**
     * 内容操作（选择、固定等）
     * 映射: POST /contents → POST /api/v1/contents
     */
    @PostMapping("/contents")
    public ApiResponse<Void> postContents(
            @RequestParam("path") @NotBlank String path,
            @RequestParam("courseId") @Positive Long courseId,
            @RequestParam("postingId") @Positive Long postingId,
            @RequestParam("action") @Min(1) @Max(4) int action,
            Model model) {

        long userId = StpUtil.getLoginIdAsLong();

        switch (action) {
            case 1:
                contentsService.choose(userId, path, courseId, postingId);
                break;
            case 2:
                contentsService.unchoose(userId, courseId, path);
                break;
            case 3:
                contentsService.pin(userId, courseId, path, postingId, true);
                break;
            case 4:
                contentsService.pin(userId, courseId, path, postingId, false);
                break;
            default:
                throw ErrorCode.NOT_SUPPORTED.exception();
        }
        return ApiResponse.success();
    }
}