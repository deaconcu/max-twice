package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.basic.MessageService;
import com.prosper.learn.dto.message.MessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息管理接口
 * 从AggregateClient拆分出的消息功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessagesController {

    private final MessageService messageService;

    /**
     * 申请课程
     * 映射: POST /message/new-course → POST /api/v1/messages/course-applications
     */
    @PostMapping("/messages/course-applications")
    public ApiResponse<Void> applyCourse(
            @RequestParam String title, 
            @RequestParam String summary, 
            @RequestParam String explanation, 
            @RequestParam Long parentId) {
        
        long userId = StpUtil.getLoginIdAsLong();
        messageService.applyCourse(title, summary, explanation, parentId, userId);
        return ApiResponse.success();
    }

    /**
     * 获取课程申请列表
     * 映射: GET /message/new-course → GET /api/v1/messages/course-applications
     */
    @GetMapping("/messages/course-applications")
    public ApiResponse<Map<String, Object>> getApplCourseList(
            @RequestParam int page, 
            @RequestParam int length) {
        
        Map<String, Object> result = messageService.getApplyCourseListWithPagination(page, length);
        return ApiResponse.success(result);
    }

    /**
     * 获取消息列表
     * 映射: GET /message → GET /api/v1/messages
     */
    @GetMapping("/messages")
    public ApiResponse<List<MessageDTO>> getMessageList(
            @RequestParam Long userId, 
            @RequestParam int type, 
            @RequestParam Long lastId, 
            @RequestParam int conversation) {
        
        long self = StpUtil.getLoginIdAsLong();
        List<MessageDTO> messageDTOList = messageService.getList(type, self, userId, lastId, conversation);
        return ApiResponse.success(messageDTOList);
    }

    /**
     * 发送系统消息
     * 映射: POST /message/system → POST /api/v1/messages/system
     */
    @PostMapping("/messages/system")
    public ApiResponse<Void> postSystemMessage(
            @RequestParam int type, 
            @RequestParam Long userId, 
            @RequestParam String content) {
        
        //messageService.createSystemMessage(type, userId, content);
        return ApiResponse.success();
    }

    /**
     * 修改课程申请
     * 映射: PUT /message/system → PUT /api/v1/messages/course-applications/{id}
     */
    @PutMapping("/messages/course-applications/{id}")
    public ApiResponse<Void> modifyCourseApply(
            @PathVariable Long id, 
            @RequestParam String reply) {
        
        messageService.modifyCourseApply(id, reply);
        return ApiResponse.success();
    }
}