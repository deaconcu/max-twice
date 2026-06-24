package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.analytics.stats.dataservice.UserStatsDataService;
import com.twicemax.application.dto.request.CreateNotificationRequest;
import com.twicemax.application.dto.response.message.MessageListResponse;
import com.twicemax.application.service.MessageService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 消息管理接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class MessagesController {

    private final MessageService messageService;
    private final UserDataService userDataService;
    private final UserStatsDataService userStatsDataService;

    @GetMapping("/messages/category")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public MessageListResponse getMessagesByCategory(
            @RequestParam @NotNull(message = "消息分类不能为空")
            @Min(value = 1, message = "消息分类必须为1-4")
            @Max(value = 4, message = "消息分类必须为1-4")
            int category,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer type,
            @CurrentUser UserDO currentUser) {
        return messageService.getListByCategoryWithLastViewed(category, currentUser.getId(), cursor, type, currentUser);
    }

    @GetMapping("/messages/unread-count")
    @SaCheckLogin
    @RateLimit(capacity = 200, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public int getUnreadCount(@CurrentUser UserDO currentUser) {
        long lastViewedMessageId = userStatsDataService.getLastViewedMessageId(currentUser.getId());
        return messageService.getUnreadCount(currentUser.getId(), lastViewedMessageId);
    }

    @PostMapping("/messages/invite")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> inviteUser(
            @RequestBody @Valid CreateNotificationRequest request,
            @CurrentUser UserDO currentUser) {
        userDataService.validateAndGet(request.getUserId());
        messageService.createInviteMessage(request.getUserId(), currentUser.getId(), request.getNodeId());
        return ResponseEntity.noContent().build();
    }
}
