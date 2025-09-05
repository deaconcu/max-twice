package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.UpvoteService;
import com.prosper.learn.dto.response.UpvoteStatusDTO;
import com.prosper.learn.dto.request.UpvoteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import static com.prosper.learn.common.Enums.ObjectType.comment;
import static com.prosper.learn.common.Enums.ObjectType.post;
import static com.prosper.learn.common.Enums.ObjectType.roadmap;

/**
 * 点赞接口
 * 从AggregateClient拆分出的点赞功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UpvotesController {

    private final UpvoteService upvoteService;

    /**
     * 点赞操作
     * 映射: POST /upvote → POST /api/v1/upvotes
     */
    @PostMapping("/upvotes")
    public ApiResponse<UpvoteStatusDTO> upvote(@RequestBody @Valid UpvoteRequest request) {
        
        long userId = StpUtil.getLoginIdAsLong();
        
        if (request.getObjectType() == post.value()) {
            upvoteService.upvotePost(request.getObjectId(), userId, request.getType());
        } else if (request.getObjectType() == comment.value()) {
            upvoteService.upvoteComment(request.getObjectId(), userId);
        } else if (request.getObjectType() == roadmap.value()) {
            upvoteService.upvoteRoadmap(request.getObjectId(), userId);
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(request.getObjectId(), request.getObjectType(), userId);
        return ApiResponse.success(result);
    }

    /**
     * 获取点赞状态
     * 映射: 新增接口 → GET /api/v1/upvotes/status?objectId=123&objectType=1
     */
    @GetMapping("/upvotes/status")
    public ApiResponse<UpvoteStatusDTO> getUpvoteStatus(
            @RequestParam Long objectId, 
            @RequestParam int objectType) {
        
        long userId = StpUtil.getLoginIdAsLong();
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(objectId, objectType, userId);
        
        return ApiResponse.success(result);
    }
}