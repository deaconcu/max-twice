package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.UpdateUserRequest;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import com.twicemax.application.dto.response.user.UserProfileDTO;
import com.twicemax.application.dto.response.user.UserPublicDTO;
import com.twicemax.application.service.ImageUploadService;
import com.twicemax.application.service.UserService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/")
@Slf4j
@RequiredArgsConstructor
@Validated
public class UsersController {

    private final UserService userService;
    private final ImageUploadService imageUploadService;

    @GetMapping("/users/current")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserProfileDTO getCurrentUser(@CurrentUser UserDO currentUser) {
        return userService.getUser(currentUser.getId());
    }

    @PutMapping("/users/current")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateCurrentUser(
            @RequestBody @Valid UpdateUserRequest request,
            @CurrentUser UserDO currentUser) {
        userService.updateCurrentUser(currentUser.getId(), request.getName(), request.getBiography(), request.getTimezone());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/avatar")
    @SaCheckLogin
    @RateLimit(capacity = 5, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String updateAvatar(
            @RequestParam("file") @NotNull(message = "文件不能为空") MultipartFile file,
            @CurrentUser UserDO currentUser) {
        log.info("用户 {} 开始更新头像", currentUser.getId());
        var response = imageUploadService.upload(file, currentUser.getId(), "avatar");
        userService.updateUserAvatar(currentUser.getId(), response.getFileUrl());
        log.info("用户 {} 头像更新成功: {}", currentUser.getId(), response.getFileUrl());
        return response.getFileUrl();
    }

    @GetMapping("/users/{username}")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public UserPublicDTO getUser(
            @PathVariable @NotBlank(message = "用户名不能为空") String username,
            @CurrentUser UserDO currentUser) {
        return userService.getUserByUsername(username, currentUser.getId());
    }

    @GetMapping("/users/search")
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.IP)
    public List<UserBriefDTO> searchUsers(@RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        return userService.searchUsers(name);
    }
}
