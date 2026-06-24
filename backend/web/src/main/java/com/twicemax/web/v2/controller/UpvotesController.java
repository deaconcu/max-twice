package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.UpvoteRequest;
import com.twicemax.application.dto.response.UpvoteStatusDTO;
import com.twicemax.application.service.UpvoteService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.ContentType.*;

/**
 * 点赞接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class UpvotesController {

    private final UpvoteService upvoteService;

    @PostMapping("/upvotes")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UpvoteStatusDTO upvote(
            @RequestBody @Valid UpvoteRequest request,
            @CurrentUser UserDO currentUser) {

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

        return upvoteService.getUpvoteStatus(
                request.getObjectId(),
                Enums.ContentType.getByValue(request.getObjectType()),
                currentUser.getId());
    }

    @GetMapping("/upvotes/status")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UpvoteStatusDTO getUpvoteStatus(
            @RequestParam @NotNull(message = "对象ID不能为空") @Positive(message = "对象ID必须大于0") Long objectId,
            @RequestParam @NotNull(message = "对象类型不能为空") @Positive(message = "对象类型必须大于0") int objectType,
            @CurrentUser UserDO currentUser) {
        return upvoteService.getUpvoteStatus(
                objectId,
                Enums.ContentType.getByValue(objectType),
                currentUser.getId());
    }
}
