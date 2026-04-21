package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.analytics.stats.dataservice.UserStatsDataService;
import com.twicemax.application.dto.request.CreateNotificationRequest;
import com.twicemax.application.dto.response.message.MessageListResponse;
import com.twicemax.application.service.MessageService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.user.profile.UserDataService;
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

/**
 * 消息管理接口
 * 从AggregateClient拆分出的消息功能
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
public class MessagesController {

    private final MessageService messageService;
    private final UserDataService userDataService;
    private final UserStatsDataService userStatsDataService;

    /**
     * 按分类获取消息列表
     * 映射: GET /api/v1/messages/category
     * @param category 消息分类 1=互动消息, 2=系统消息, 3=全部（互动+系统）, 4=私信
     * @param lastId 最后一条消息的ID，用于分页，首次请求可不传
     * @param type 可选的消息类型过滤
     */
    @GetMapping("/messages/category")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<MessageListResponse> getMessagesByCategory(
            @RequestParam @NotNull(message = "消息分类不能为空")
            @Min(value = 1, message = "消息分类必须为1-4")
            @Max(value = 4, message = "消息分类必须为1-4")
            int category,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer type,
            @CurrentUser UserDO currentUser) {

        // 获取消息列表（内部会处理 lastViewedMessageId 的读取和更新）
        MessageListResponse response = messageService.getListByCategoryWithLastViewed(
            category, currentUser.getId(), lastId, type, currentUser
        );

        return ApiResponse.success(response);
    }

    /**
     * 获取未读消息数量
     * 映射: GET /api/v1/messages/unread-count
     */
    @GetMapping("/messages/unread-count")
    @SaCheckLogin
    @RateLimit(capacity = 200, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Integer> getUnreadCount(@CurrentUser UserDO currentUser) {
        long lastViewedMessageId = userStatsDataService.getLastViewedMessageId(currentUser.getId());
        int count = messageService.getUnreadCount(currentUser.getId(), lastViewedMessageId);
        return ApiResponse.success(count);
    }

/**
     * 邀请用户
     * 映射: POST /message/invite → POST /api/v1/messages/invite
     */
    @PostMapping("/messages/invite")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> inviteUser(
            @RequestBody @Valid CreateNotificationRequest request,
            @CurrentUser UserDO currentUser) {

        UserDO userDO = userDataService.validateAndGet(request.getUserId());

        messageService.createInviteMessage(request.getUserId(), currentUser.getId(), request.getNodeId());
        return ApiResponse.success();
    }
}