package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.MessageService;
import com.prosper.learn.dto.message.MessageDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    private final CourseMapper courseMapper;

    /**
     * 申请课程
     * 映射: POST /message/new-course → POST /api/v1/messages/course-applications
     */
    @PostMapping("/messages/course-applications")
    public ResponseEntity<ApiResponse<Void>> applyCourse(
            @RequestParam String title, 
            @RequestParam String summary, 
            @RequestParam String explanation, 
            @RequestParam Long parentId) {
        
        int userId = StpUtil.getLoginIdAsInt();
        CourseDO course = null;
        if (parentId != 0) {
            course = courseMapper.getById(parentId);
            if (course == null) {
                throw new RuntimeException("course not found");
            }
        }

        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("summary", summary);
        data.put("explanation", explanation);
        data.put("parentId", Long.toString(parentId));
        if (course != null) {
            data.put("parentName", course.getName());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        messageService.create(jsonString, userId, 0, Enums.MessageType.applyCourse);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 获取课程申请列表
     * 映射: GET /message/new-course → GET /api/v1/messages/course-applications
     */
    @GetMapping("/messages/course-applications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getApplCourseList(
            @RequestParam int page, 
            @RequestParam int length) {
        
        if (page < 1) page = 1;
        if (length < 1) length = 1;
        if (length > 100) length = 100;
        int count = messageService.getApplyCourseCount();
        int totalPage = count / length + 1;
        if (page > totalPage) page = totalPage;

        Map<String, Object> resultMap = new HashMap<>();
        List<MessageDTO> messageDTOList = messageService.getApplyCourseMessage(page, length);
        resultMap.put("messages", messageDTOList);

        Map<String, Integer> pagination = new HashMap<>();
        pagination.put("total", count);
        pagination.put("pageSize", length);
        pagination.put("currentPage", page);
        pagination.put("totalPages", totalPage);
        resultMap.put("pagination", pagination);
        return ResponseEntity.ok(ApiResponse.success(resultMap));
    }

    /**
     * 获取消息列表
     * 映射: GET /message → GET /api/v1/messages
     */
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessageList(
            @RequestParam Long userId, 
            @RequestParam int type, 
            @RequestParam Long lastId, 
            @RequestParam int conversation) {
        
        int self = StpUtil.getLoginIdAsInt();
        List<MessageDTO> messageDTOList = messageService.getList(type, self, userId, lastId, conversation);
        return ResponseEntity.ok(ApiResponse.success(messageDTOList));
    }

    /**
     * 发送系统消息
     * 映射: POST /message/system → POST /api/v1/messages/system
     */
    @PostMapping("/messages/system")
    public ResponseEntity<ApiResponse<Void>> postSystemMessage(
            @RequestParam int type, 
            @RequestParam Long userId, 
            @RequestParam String content) {
        
        //messageService.createSystemMessage(type, userId, content);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 修改课程申请
     * 映射: PUT /message/system → PUT /api/v1/messages/course-applications/{id}
     */
    @PutMapping("/messages/course-applications/{id}")
    public ResponseEntity<ApiResponse<Void>> modifyCourseApply(
            @PathVariable Long id, 
            @RequestParam String reply) {
        
        messageService.modifyCourseApply(id, reply);
        return ResponseEntity.ok(ApiResponse.success());
    }
}