package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.ContentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 内容管理接口
 * 从AggregateClient拆分出的内容管理功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentsController {

    private final ContentsService contentsService;

    /**
     * 内容操作（选择、固定等）
     * 映射: POST /contents → POST /api/v1/contents
     */
    @PostMapping("/contents")
    public ResponseEntity<ApiResponse<Void>> postContents(
            @RequestParam("path") String path,
            @RequestParam("courseId") Long courseId,
            @RequestParam("postingId") Long postingId,
            @RequestParam("action") int action,
            Model model) {
        
        int userId = StpUtil.getLoginIdAsInt();
        switch (action) {
            case 1:
                contentsService.choose(userId, path, courseId, postingId);
                break;
            case 2:
                contentsService.unchoose(userId, courseId, path);
                break;
            case 3:
                contentsService.pin(userId, courseId, path, postingId, true);
                break;
            case 4:
                contentsService.pin(userId, courseId, path, postingId, false);
                break;
        }
        return ResponseEntity.ok(ApiResponse.success());
    }
}