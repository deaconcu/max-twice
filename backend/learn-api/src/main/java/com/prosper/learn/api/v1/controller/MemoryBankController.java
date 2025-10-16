package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.business.MemoryBankService;
import com.prosper.learn.dto.request.AddDeckToMemoryBankRequest;
import com.prosper.learn.dto.request.RemoveDeckFromCourseRequest;
import com.prosper.learn.dto.request.UpdateCourseSettingRequest;
import com.prosper.learn.dto.response.CourseMemoryBankDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 记忆库管理控制器
 */
@RestController
@RequestMapping("/api/v1/memory/memory-bank")
@RequiredArgsConstructor
@Slf4j
@Validated
public class MemoryBankController {

    private final MemoryBankService memoryBankService;

    /**
     * 添加卡片组到记忆库
     */
    @PostMapping("/decks")
    public ApiResponse<Void> addDeckToMemoryBank(@Valid @RequestBody AddDeckToMemoryBankRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        memoryBankService.addDeckToMemoryBank(userId, request);
        return ApiResponse.success();
    }

    /**
     * 获取记忆库课程列表
     */
    @GetMapping("/courses")
    public ApiResponse<List<CourseMemoryBankDTO>> getMemoryBankCourses(
            @RequestParam(required = false) @Min(value = 0, message = "状态不能小于0") Integer status) {
        
        long userId = StpUtil.getLoginIdAsLong();
        List<CourseMemoryBankDTO> result = memoryBankService.getMemoryBankCourses(userId, status);
        return ApiResponse.success(result);
    }

    /**
     * 更新课程复习策略
     */
    @PutMapping("/courses/{courseId}/settings")
    public ApiResponse<Void> updateCourseSetting(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @Valid @RequestBody UpdateCourseSettingRequest request) {
        
        long userId = StpUtil.getLoginIdAsLong();
        memoryBankService.updateCourseSetting(userId, courseId, request);
        return ApiResponse.success();
    }

    /**
     * 移除卡片组
     */
    @DeleteMapping("/courses/{courseId}/decks/{deckId}")
    public ApiResponse<Void> removeDeckFromCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId) {

        long userId = StpUtil.getLoginIdAsLong();
        memoryBankService.removeDeckFromCourse(userId, courseId, deckId);
        return ApiResponse.success();
    }


}