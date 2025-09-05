package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.business.UpvoteService;
import com.prosper.learn.dto.response.UpvoteStatusDTO;
import lombok.RequiredArgsConstructor;
import com.prosper.learn.api.v1.annotation.JsonParam;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<UpvoteStatusDTO> upvote(
            @JsonParam("objectId") Long objectId,
            @JsonParam("objectType") Integer objectType,
            @JsonParam("type") Integer type) {
        
        long userId = StpUtil.getLoginIdAsLong();
        
        if (objectType == post.value()) {
            upvoteService.upvotePost(objectId, userId, type);
        } else if (objectType == comment.value()) {
            upvoteService.upvoteComment(objectId, userId);
        } else if (objectType == roadmap.value()) {
            upvoteService.upvoteRoadmap(objectId, userId);
        } else {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
        
        UpvoteStatusDTO result = upvoteService.getUpvoteStatus(objectId, objectType, userId);
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