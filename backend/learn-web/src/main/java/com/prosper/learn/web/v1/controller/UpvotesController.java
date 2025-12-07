package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.UpvoteRequest;
import com.prosper.learn.application.dto.response.UpvoteStatusDTO;
import com.prosper.learn.application.service.ContentInteractionService;
import com.prosper.learn.application.service.UpvoteService;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.concurrent.TimeUnit;

import static com.prosper.learn.shared.domain.Enums.*;
import static com.prosper.learn.shared.domain.Enums.ContentType.*;


/**
 * 点赞接口
 * 从AggregateClient拆分出的点赞功能
 *
 * 架构说明：
 * - 复杂的写操作（点赞）通过 ContentInteractionService（应用服务）处理
 * - 点赞状态查询通过 UpvoteService（应用服务）处理
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class UpvotesController {

    /** 内容交互应用服务 - 处理复杂的写操作 */
    private final ContentInteractionService contentInteractionService;

    /** 点赞应用服务 - 处理点赞状态查询 */
    private final UpvoteService upvoteService;

    /**
     * 点赞操作
     * 映射: POST /upvote → POST /api/v1/upvotes
     *
     * 使用应用服务处理复杂的编排逻辑（验证 + 业务逻辑 + 事件发布）
     */
    @PostMapping("/upvotes")
    @SaCheckLogin
    public ApiResponse<UpvoteStatusDTO> upvote(
            @RequestBody @Valid UpvoteRequest request,
            @CurrentUser UserDO currentUser) {

        // 使用应用服务处理复杂的点赞逻辑
        if (request.getObjectType() == post.value()) {
            contentInteractionService.upvotePost(request.getObjectId(), currentUser, request.getType());
        } else if (request.getObjectType() == comment.value()) {
            contentInteractionService.upvoteComment(request.getObjectId(), currentUser);
        } else if (request.getObjectType() == roadmap.value()) {
            contentInteractionService.upvoteRoadmap(request.getObjectId(), currentUser);
        } else if (request.getObjectType() == memory_card_deck.value()) {
            contentInteractionService.upvoteMemoryCardDeck(request.getObjectId(), currentUser);
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }

        // 查询操作使用 UpvoteService
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(
            request.getObjectId(),
            ContentType.getByValue(request.getObjectType()),
            currentUser.getId()
        );
        return ApiResponse.success(result);
    }

    /**
     * 获取点赞状态
     * 映射: 新增接口 → GET /api/v1/upvotes/status?objectId=123&objectType=1
     *
     * 简单查询操作直接调用 UpvoteService
     */
    @GetMapping("/upvotes/status")
    @SaCheckLogin
    public ApiResponse<UpvoteStatusDTO> getUpvoteStatus(
            @RequestParam @NotNull(message = "对象ID不能为空")
            @Positive(message = "对象ID必须大于0")
            Long objectId,
            @RequestParam @NotNull(message = "对象类型不能为空")
            @Positive(message = "对象类型必须大于0")
            int objectType,
            @CurrentUser UserDO currentUser) {

        // 查询使用 UpvoteService
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(
            objectId,
            ContentType.getByValue(objectType),
            currentUser.getId()
        );

        return ApiResponse.success(result);
    }
}