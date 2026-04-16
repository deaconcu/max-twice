package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.ApiResponse;
import com.prosper.learn.application.dto.response.home.HomePageDTO;
import com.prosper.learn.application.service.HomePageService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 首页接口
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class HomeController {

    private final HomePageService homePageService;

    /**
     * 获取首页聚合数据
     * GET /api/v1/home
     *
     * @param currentUser 当前登录用户
     * @return 首页聚合数据
     */
    @GetMapping("/home")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<HomePageDTO> getHomePageData(@CurrentUser UserDO currentUser) {
        HomePageDTO data = homePageService.getHomePageData(currentUser);
        return ApiResponse.success(data);
    }
}
