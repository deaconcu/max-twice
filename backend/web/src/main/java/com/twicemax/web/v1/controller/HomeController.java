package com.twicemax.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.ApiResponse;
import com.twicemax.application.dto.response.home.HomePageDTO;
import com.twicemax.application.service.HomePageService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v1.annotation.CurrentUser;
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
@RequestMapping("/v1")
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
