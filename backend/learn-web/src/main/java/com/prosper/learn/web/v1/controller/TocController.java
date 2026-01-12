package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.content.toc.UserCourseTocDO;
import com.prosper.learn.content.toc.UserCourseTocDataService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 课程目录接口
 * 处理用户课程目录的相关操作
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class TocController {

    private final TocDomainService tocDomainService;
    private final UserCourseTocDataService userCourseTocDataService;

    /**
     * 更新用户课程目录
     * 映射: POST /toc → PUT /api/v1/users/current/courses/{courseId}/toc
     */
    @PutMapping("/users/current/courses/{courseId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateUserCourseToc(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @JsonParam("indexArray") @NotBlank(message = "索引数组不能为空") String indexArray,
            @CurrentUser UserDO currentUser) {

        tocDomainService.updateUserCourseToc(currentUser.getId(), courseId, indexArray);
        return ApiResponse.success("目录更新成功", null);
    }

    /**
     * 获取用户课程目录
     * 新增接口: GET /api/v1/users/current/courses/{courseId}/toc
     */
    @GetMapping("/users/current/courses/{courseId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> getUserCourseToc(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0")
            Long courseId,
            @CurrentUser UserDO currentUser) {

        UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(currentUser.getId(), courseId);
        if (userCourseTocDO == null) {
            return ApiResponse.success(null);
        }

        return ApiResponse.success(userCourseTocDO.getToc());
    }
}