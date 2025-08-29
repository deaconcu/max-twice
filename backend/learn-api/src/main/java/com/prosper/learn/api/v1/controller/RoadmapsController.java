package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.RoadmapService;
import com.prosper.learn.dto.RoadmapDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;

import java.util.List;

/**
 * 路线图接口
 * 从RoadmapClient迁移而来
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoadmapsController {

    private final RoadmapService roadmapService;

    /**
     * 获取职业路线图
     * 映射: GET /roadmap/list/{professionId} → GET /api/v1/professions/{professionId}/roadmaps?lastId=123
     */
    @GetMapping("/professions/{professionId}/roadmaps")
    public ApiResponse<List<RoadmapDTO>> getRoadmapsByProfession(
            @PathVariable Long professionId, 
            @RequestParam(required = false, defaultValue = "0") Long lastId) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        List<RoadmapDTO> roadmaps = roadmapService.getRoadmapsByProfession(professionId, lastId, userId);

        return ApiResponse.success(roadmaps);
    }

    /**
     * 更新路线图
     * 映射: PUT /roadmap/{id} → PUT /api/v1/roadmaps/{id}
     */
    @PutMapping("/roadmaps/{id}")
    public ApiResponse<Void> updateRoadmap(@PathVariable Long id, @RequestParam String content) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        roadmapService.updateRoadmap(id, content, userId);
        return ApiResponse.success();
    }

    /**
     * 路线图点赞
     * 映射: PUT /roadmap/{id}/upvote → PUT /api/v1/roadmaps/{id}/upvote
     */
    @PutMapping("/roadmaps/{id}/upvote")
    public ApiResponse<RoadmapDTO> upvoteRoadmap(@PathVariable Long id) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        RoadmapDTO roadmapDTO = roadmapService.upvoteRoadmap(id, userId);

        return ApiResponse.success(roadmapDTO);
    }

    /**
     * 创建路线图
     * 映射: POST /roadmap → POST /api/v1/roadmaps
     */
    @PostMapping("/roadmaps")
    public ApiResponse<Long> createRoadmap(
            @RequestParam Long professionId, 
            @RequestParam String content, 
            @RequestParam String description) {
        
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        Long roadmapId = roadmapService.createRoadmap(professionId, content, description, userId);
        return ApiResponse.success(roadmapId);
    }

    /**
     * 获取路线图详情
     * 映射: GET /roadmap/{id} → GET /api/v1/roadmaps/{id}
     */
    @GetMapping("/roadmaps/{id}")
    public ApiResponse<RoadmapDTO> getRoadmap(@PathVariable Long id) {
        long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : 0;
        RoadmapDTO roadmapDTO = roadmapService.getRoadmapWithContent(id, userId);

        return ApiResponse.success(roadmapDTO);
    }

    /**
     * 置顶路线图
     * 映射: POST /roadmap/pin → POST /api/v1/roadmaps/pin
     */
    @PostMapping("/roadmaps/pin")
    public ApiResponse<String> pinRoadmap(@RequestParam Long professionId, @RequestParam Long roadmapId) {
        if (!StpUtil.isLogin()) {
            throw ErrorCode.USER_NOT_LOGIN.exception();
        }

        long userId = StpUtil.getLoginIdAsLong();
        String result = roadmapService.pinRoadmap(professionId, roadmapId, userId);

        return ApiResponse.success(result);
    }
}