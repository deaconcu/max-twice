package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.UpvoteService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CommentDTO;
import com.prosper.learn.dto.PostDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import com.prosper.learn.persistence.dataobject.UpvoteDO;
import com.prosper.learn.persistence.mapper.CommentMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import com.prosper.learn.persistence.mapper.UpvoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static com.prosper.learn.common.Enums.ObjectType.comment;
import static com.prosper.learn.common.Enums.ObjectType.post;

/**
 * 点赞接口
 * 从AggregateClient拆分出的点赞功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UpvotesController {

    private final UpvoteService upvoteService;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final UpvoteMapper upvoteMapper;

    /**
     * 点赞操作
     * 映射: POST /upvote → POST /api/v1/upvotes
     */
    @PostMapping("/upvotes")
    public ResponseEntity<ApiResponse<Object>> upvote(
            @RequestParam Long objectId, 
            @RequestParam int objectType, 
            @RequestParam int type) {
        
        if (objectType == post.value()) {
            long postId = objectId;
            long userId = StpUtil.getLoginIdAsLong();
            upvoteService.upvotePost(postId, userId, type);

            PostDTO postDTO = Converter.INSTANCE.toPostDTO(postMapper.get(postId));
            UpvoteDO upvoteDO = upvoteMapper.get(userId, postId, post.value());
            if (upvoteDO != null) {
                postDTO.setVoteType(upvoteDO.getType());
            }
            return ResponseEntity.ok(ApiResponse.success(postDTO));
        } else if (objectType == comment.value()) {
            long commentId = objectId;
            long userId = StpUtil.getLoginIdAsLong();
            upvoteService.upvoteComment(commentId, userId);

            CommentDO commentDO = commentMapper.get(commentId);
            CommentDTO commentDTO = Converter.INSTANCE.toCommentDTO(commentDO);
            UpvoteDO upvoteDO = upvoteMapper.get(userId, commentId, comment.value());
            if (upvoteDO != null) {
                commentDTO.setUpvoted(1);
            }
            return ResponseEntity.ok(ApiResponse.success(commentDTO));
        } else {
            throw new IllegalArgumentException("不支持的对象类型");
        }
    }

    /**
     * 获取点赞状态
     * 映射: 新增接口 → GET /api/v1/upvotes/status?objectId=123&objectType=1
     */
    @GetMapping("/upvotes/status")
    public ResponseEntity<ApiResponse<Object>> getUpvoteStatus(
            @RequestParam Long objectId, 
            @RequestParam int objectType) {
        
        long userId = StpUtil.getLoginIdAsLong();
        UpvoteDO upvoteDO = upvoteMapper.get(userId, objectId, objectType);
        
        Map<String, Object> result = new HashMap<>();
        result.put("objectId", objectId);
        result.put("objectType", objectType);
        result.put("upvoted", upvoteDO != null);
        if (upvoteDO != null) {
            result.put("type", upvoteDO.getType());
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}