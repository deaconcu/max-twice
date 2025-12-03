package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.business.service.application.ContentInteractionService;
import com.prosper.learn.business.service.domain.ContentStatsDomainService;
import com.prosper.learn.dto.response.UpvoteStatusDTO;
import com.prosper.learn.dto.request.UpvoteRequest;
import com.prosper.learn.persistence.dataobject.UserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.concurrent.TimeUnit;

import static com.prosper.learn.common.Enums.ContentType.comment;
import static com.prosper.learn.common.Enums.ContentType.post;
import static com.prosper.learn.common.Enums.ContentType.roadmap;
import static com.prosper.learn.common.Enums.ContentType.memory_card_deck;

/**
 * 点赞接口
 * 从AggregateClient拆分出的点赞功能
 *
 * 架构说明：
 * - 复杂的写操作（点赞）通过 ContentInteractionService（应用服务）处理
 * - 简单的查询操作直接调用 ContentStatsService（领域服务）
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class UpvotesController {

    /** 内容交互应用服务 - 处理复杂的写操作 */
    private final ContentInteractionService contentInteractionService;

    /** 内容统计业务服务 - 处理简单的查询操作 */
    private final ContentStatsDomainService contentStatsDomainService;

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

        // 查询操作直接使用业务服务
        UpvoteStatusDTO result = contentStatsDomainService.getUpvoteStatus(request.getObjectId(), request.getObjectType(), currentUser.getId());
        return ApiResponse.success(result);
    }

    /**
     * 获取点赞状态
     * 映射: 新增接口 → GET /api/v1/upvotes/status?objectId=123&objectType=1
     *
     * 简单查询操作直接调用业务服务
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

        // 简单查询直接使用业务服务，无需应用服务包装
        UpvoteStatusDTO result = contentStatsDomainService.getUpvoteStatus(objectId, objectType, currentUser.getId());

        return ApiResponse.success(result);
    }
}