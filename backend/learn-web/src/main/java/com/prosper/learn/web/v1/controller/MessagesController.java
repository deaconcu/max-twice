package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.JsonParam;
import com.prosper.learn.web.v1.dto.ApiResponse;
import com.prosper.learn.business.service.domain.MessageDomainService;
import com.prosper.learn.dto.response.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息管理接口
 * 从AggregateClient拆分出的消息功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class MessagesController {

    private final MessageDomainService messageDomainService;
    private final UserMapper userMapper;

    /**
     * 申请课程
     * 映射: POST /message/new-course → POST /api/v1/messages/course-applications
     */
    @PostMapping("/messages/course-applications")
    @SaCheckLogin
    public ApiResponse<Void> applyCourse(
            @RequestBody @Valid CreateMessageRequest request,
            @CurrentUser UserDO currentUser) {

        messageDomainService.applyCourse(request.getTitle(), request.getSummary(), request.getExplanation(), request.getParentId(), currentUser.getId());
        return ApiResponse.success();
    }

    /**
     * 获取消息列表
     * 映射: GET /message → GET /api/v1/messages
     */
    @GetMapping("/messages/system")
    @SaCheckLogin
    public ApiResponse<List<MessageDTO>> getSystemMessageList(
            @RequestParam @NotNull(message = "消息类型不能为空")
            @Positive(message = "消息类型必须大于0")
            int type,
            @RequestParam @NotNull(message = "最后ID不能为空")
            @Min(value = 0, message = "最后ID不能小于0")
            Long lastId,
            @CurrentUser UserDO currentUser) {

        List<MessageDTO> messageDTOList = messageDomainService.getSystemList(type, currentUser.getId(), lastId);
        return ApiResponse.success(messageDTOList);
    }

    /**
     * 按分类获取消息列表
     * 映射: GET /api/v1/messages/category
     * @param category 消息分类 1=互动消息, 2=系统消息, 3=私信
     * @param lastId 最后一条消息的ID，用于分页，首次请求可不传
     * @param type 可选的消息类型过滤
     */
    @GetMapping("/messages/category")
    @SaCheckLogin
    public ApiResponse<List<MessageDTO>> getMessagesByCategory(
            @RequestParam @NotNull(message = "消息分类不能为空")
            @Min(value = 1, message = "消息分类必须为1-3")
            @Max(value = 3, message = "消息分类必须为1-3")
            int category,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer type,
            @CurrentUser UserDO currentUser) {

        List<MessageDTO> messageDTOList = messageDomainService.getListByCategory(category, currentUser.getId(), lastId, type);
        return ApiResponse.success(messageDTOList);
    }

    /**
     * 获取消息列表
     * 映射: GET /message → GET /api/v1/messages
     */
    @GetMapping("/messages")
    @SaCheckLogin
    public ApiResponse<List<MessageDTO>> getMessageList(
            @RequestParam @NotNull(message = "用户ID不能为空")
            @Positive(message = "用户ID必须大于0")
            Long userId,
            @RequestParam @NotNull(message = "消息类型不能为空")
            @Positive(message = "消息类型必须大于0")
            int type,
            @RequestParam @NotNull(message = "最后ID不能为空")
            @Min(value = 0, message = "最后ID不能小于0")
            Long lastId,
            @RequestParam @NotNull(message = "会话类型不能为空")
            @Min(value = 0, message = "会话类型不能小于0")
            int conversation,
            @CurrentUser UserDO currentUser) {

        List<MessageDTO> messageDTOList = messageDomainService.getList(type, currentUser.getId(), userId, lastId, conversation);
        return ApiResponse.success(messageDTOList);
    }

    /**
     * 发送系统消息
     * 映射: POST /message/system → POST /api/v1/messages/system
     */
    @PostMapping("/messages/system")
    public ApiResponse<Void> postSystemMessage(@RequestBody @Valid SendMessageRequest request) {

        //messageService.createSystemMessage(request.getType(), request.getUserId(), request.getContent());
        return ApiResponse.success();
    }

    /**
     * 修改课程申请
     * 映射: PUT /message/system → PUT /api/v1/messages/course-applications/{id}
     */
    @PutMapping("/messages/course-applications/{id}")
    public ApiResponse<Void> modifyCourseApply(
            @PathVariable @NotNull(message = "申请ID不能为空")
            @Positive(message = "申请ID必须大于0")
            Long id,
            @JsonParam("reply") @NotBlank(message = "回复内容不能为空") String reply) {

        messageDomainService.modifyCourseApply(id, reply);
        return ApiResponse.success();
    }

    /**
     * 邀请用户
     * 映射: POST /message/invite → POST /api/v1/messages/invite
     */
    @PostMapping("/messages/invite")
    @SaCheckLogin
    public ApiResponse<Void> inviteUser(
            @RequestBody @Valid CreateNotificationRequest request,
            @CurrentUser UserDO currentUser) {

        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        if (request.getNodeId() <= 0) {
            throw new IllegalArgumentException("节点ID无效");
        }

        UserDO userDO = userMapper.getById(request.getUserId());
        if (userDO == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        messageDomainService.createInviteMessage(request.getUserId(), currentUser.getId(), request.getNodeId());
        return ApiResponse.success();
    }
}