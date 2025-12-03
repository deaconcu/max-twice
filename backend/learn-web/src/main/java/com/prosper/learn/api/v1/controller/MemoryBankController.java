package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.business.service.application.MemoryBankService;
import com.prosper.learn.dto.request.AddDeckToMemoryBankRequest;
import com.prosper.learn.dto.request.UpdateCourseSettingRequest;
import com.prosper.learn.dto.response.CourseMemoryBankDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 记忆库管理控制器
 */
@RestController
@RequestMapping("/api/v1/memory/memory-bank")
@RequiredArgsConstructor
@Slf4j
@Validated
@RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class MemoryBankController {

    private final MemoryBankService memoryBankService;

    /**
     * 添加卡片组到记忆库
     */
    @PostMapping("/decks")
    @SaCheckLogin
    public ApiResponse<Void> addDeckToMemoryBank(
            @Valid @RequestBody AddDeckToMemoryBankRequest request,
            @CurrentUser UserDO currentUser) {
        memoryBankService.addDeckToMemoryBank(currentUser.getId(), request);
        return ApiResponse.success();
    }

    /**
     * 获取记忆库课程列表
     */
    @GetMapping("/courses")
    @SaCheckLogin
    public ApiResponse<List<CourseMemoryBankDTO>> getMemoryBankCourses(
            @RequestParam(required = false) @Min(value = 0, message = "状态不能小于0") Integer status,
            @CurrentUser UserDO currentUser) {

        List<CourseMemoryBankDTO> result = memoryBankService.getMemoryBankCourses(currentUser.getId(), status);
        return ApiResponse.success(result);
    }

    /**
     * 更新课程复习策略
     */
    @PutMapping("/courses/{courseId}/settings")
    @SaCheckLogin
    public ApiResponse<Void> updateCourseSetting(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @Valid @RequestBody UpdateCourseSettingRequest request,
            @CurrentUser UserDO currentUser) {

        memoryBankService.updateCourseSetting(currentUser.getId(), courseId, request);
        return ApiResponse.success();
    }

    /**
     * 移除卡片组
     */
    @DeleteMapping("/courses/{courseId}/decks/{deckId}")
    @SaCheckLogin
    public ApiResponse<Void> removeDeckFromCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @PathVariable @NotNull(message = "卡片组ID不能为空")
            @Positive(message = "卡片组ID必须大于0")
            Long deckId,
            @CurrentUser UserDO currentUser) {

        memoryBankService.removeDeckFromCourse(currentUser.getId(), courseId, deckId);
        return ApiResponse.success();
    }


}