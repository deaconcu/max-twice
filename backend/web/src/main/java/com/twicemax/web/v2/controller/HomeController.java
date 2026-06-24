package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.response.home.HomePageDTO;
import com.twicemax.application.service.HomePageService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 首页接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

    private final HomePageService homePageService;

    @GetMapping("/home")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public HomePageDTO getHomePageData(@CurrentUser UserDO currentUser) {
        return homePageService.getHomePageData(currentUser);
    }
}
