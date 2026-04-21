package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.request.MarkImageUsedRequest;
import com.twicemax.application.dto.response.ImageUploadHistoryDTO;
import com.twicemax.application.dto.response.ImageUploadResponse;
import com.twicemax.application.dto.response.QuotaUsageDTO;
import com.twicemax.application.service.ImageUploadService;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
import com.twicemax.web.v1.annotation.RequireRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图片上传接口
 */
@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    /**
     * 上传图片
     * POST /api/v1/images/upload
     *
     * @param file 图片文件
     * @param refType 引用类型：post/comment/avatar/course/roadmap
     * @param currentUser 当前用户
     * @return 图片URL
     */
    @PostMapping("/upload")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<ImageUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("refType") @NotBlank(message = "引用类型不能为空") String refType,
            @CurrentUser UserDO currentUser) {

        log.info("用户{}上传图片，类型：{}", currentUser.getId(), refType);
        ImageUploadResponse response = imageUploadService.upload(file, currentUser.getId(), refType);
        return ApiResponse.success(response);
    }

    /**
     * 标记图片为使用中（管理员维护接口）
     * POST /api/v1/images/mark-used
     *
     * @param request 标记请求
     * @param currentUser 当前用户
     * @return 成功响应
     */
    @PostMapping("/mark-used")
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> markAsUsed(
            @Valid @RequestBody MarkImageUsedRequest request,
            @CurrentUser UserDO currentUser) {

        log.info("管理员{}标记图片为使用中，数量：{}", currentUser.getId(), request.getFileUrls().size());
        imageUploadService.markAsUsed(request);
        return ApiResponse.success();
    }

    /**
     * 删除图片（管理员维护接口）
     * DELETE /api/v1/images
     *
     * @param fileUrl 图片URL
     * @param currentUser 当前用户
     * @return 成功响应
     */
    @DeleteMapping
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> delete(
            @RequestParam("fileUrl") @NotBlank(message = "图片URL不能为空") String fileUrl,
            @CurrentUser UserDO currentUser) {

        log.info("用户{}删除图片：{}", currentUser.getId(), fileUrl);
        imageUploadService.delete(fileUrl, currentUser.getId());
        return ApiResponse.success();
    }

    /**
     * 获取配额使用情况
     * GET /api/v1/images/quota
     *
     * @param currentUser 当前用户
     * @return 配额使用情况
     */
    @GetMapping("/quota")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<QuotaUsageDTO> getQuota(@CurrentUser UserDO currentUser) {
        QuotaUsageDTO quota = imageUploadService.getQuotaUsage(currentUser.getId());
        return ApiResponse.success(quota);
    }

    /**
     * 获取上传历史
     * GET /api/v1/images/history
     *
     * @param limit 返回数量
     * @param currentUser 当前用户
     * @return 上传历史列表
     */
    @GetMapping("/history")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<ImageUploadHistoryDTO>> getHistory(
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @CurrentUser UserDO currentUser) {

        List<ImageUploadHistoryDTO> history = imageUploadService.getUploadHistory(currentUser.getId(), limit);
        return ApiResponse.success(history);
    }
}
