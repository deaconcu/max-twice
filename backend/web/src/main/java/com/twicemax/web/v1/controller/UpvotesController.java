package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.UpvoteRequest;
import com.twicemax.application.dto.response.UpvoteStatusDTO;
import com.twicemax.application.service.UpvoteService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
import com.twicemax.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.ContentType.*;


/**
 * 点赞接口
 * 从AggregateClient拆分出的点赞功能
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
public class UpvotesController {

    private final UpvoteService upvoteService;

    /**
     * 点赞操作
     * 映射: POST /upvote → POST /api/v1/upvotes
     */
    @PostMapping("/upvotes")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<UpvoteStatusDTO> upvote(
            @RequestBody @Valid UpvoteRequest request,
            @CurrentUser UserDO currentUser) {

        // 直接调用 UpvoteService 处理点赞逻辑
        if (request.getObjectType() == post.value()) {
            upvoteService.upvotePost(request.getObjectId(), currentUser, request.getType());
        } else if (request.getObjectType() == comment.value()) {
            upvoteService.upvoteComment(request.getObjectId(), currentUser);
        } else if (request.getObjectType() == roadmap.value()) {
            upvoteService.upvoteRoadmap(request.getObjectId(), currentUser);
        } else if (request.getObjectType() == memory_card_deck.value()) {
            upvoteService.upvoteMemoryCardDeck(request.getObjectId(), currentUser);
        } else {
            throw StatusCode.INVALID_PARAMETER.exception();
        }

        // 查询操作使用 UpvoteService
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(
            request.getObjectId(),
            Enums.ContentType.getByValue(request.getObjectType()),
            currentUser.getId()
        );
        return ApiResponse.success(result);
    }

    /**
     * 获取点赞状态
     * 映射: 新增接口 → GET /api/v1/upvotes/status?objectId=123&objectType=1
     */
    @GetMapping("/upvotes/status")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
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
            Enums.ContentType.getByValue(objectType),
            currentUser.getId()
        );

        return ApiResponse.success(result);
    }
}