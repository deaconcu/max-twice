package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.ReviewCardRequest;
import com.twicemax.application.dto.response.ReviewSubmitResultDTO;
import com.twicemax.application.dto.response.card.CardWithSrsDTO;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.ReviewService;
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

/**
 * 复习功能控制器
 */
@RestController
@RequestMapping("/memory/review")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/cards")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<CardWithSrsDTO> getCardList(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @RequestParam(required = false) String cursor,
            @CurrentUser UserDO currentUser) {
        return reviewService.getCardList(currentUser, courseId, cursor);
    }

    @GetMapping("/next")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ReviewSubmitResultDTO getNextCard(
            @RequestParam(required = false) @Positive(message = "课程ID必须大于0") Long courseId,
            @CurrentUser UserDO currentUser) {
        return reviewService.getNextCard(currentUser, courseId);
    }

    @PostMapping("/submit")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ReviewSubmitResultDTO submitReview(
            @Valid @RequestBody ReviewCardRequest request,
            @CurrentUser UserDO currentUser) {
        return reviewService.submitReview(currentUser, request);
    }
}
