package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.MarkImageUsedRequest;
import com.twicemax.application.dto.response.ImageUploadHistoryDTO;
import com.twicemax.application.dto.response.ImageUploadResponse;
import com.twicemax.application.dto.response.QuotaUsageDTO;
import com.twicemax.application.service.ImageUploadService;
import com.twicemax.shared.domain.Enums.UserRole;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.RequireRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图片上传接口
 */
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/upload")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ImageUploadResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("refType") @NotBlank(message = "引用类型不能为空") String refType,
            @CurrentUser UserDO currentUser) {
        log.info("用户{}上传图片，类型：{}", currentUser.getId(), refType);
        return imageUploadService.upload(file, currentUser.getId(), refType);
    }

    @PostMapping("/mark-used")
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> markAsUsed(
            @Valid @RequestBody MarkImageUsedRequest request,
            @CurrentUser UserDO currentUser) {
        log.info("管理员{}标记图片为使用中，数量：{}", currentUser.getId(), request.getFileUrls().size());
        imageUploadService.markAsUsed(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @RequireRole(UserRole.ADMIN)
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> delete(
            @RequestParam("fileUrl") @NotBlank(message = "图片URL不能为空") String fileUrl,
            @CurrentUser UserDO currentUser) {
        log.info("用户{}删除图片：{}", currentUser.getId(), fileUrl);
        imageUploadService.delete(fileUrl, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quota")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public QuotaUsageDTO getQuota(@CurrentUser UserDO currentUser) {
        return imageUploadService.getQuotaUsage(currentUser.getId());
    }

    @GetMapping("/history")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<ImageUploadHistoryDTO> getHistory(
            @RequestParam(defaultValue = "20") int limit,
            @CurrentUser UserDO currentUser) {
        return imageUploadService.getUploadHistory(currentUser.getId(), limit);
    }
}
