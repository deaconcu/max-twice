package com.prosper.learn.web.v1.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.infrastructure.context.RequestContext;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器
 * 在请求开始时将当前用户存入 RequestContext
 * 在请求结束时清理 RequestContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestContextInterceptor implements HandlerInterceptor {

    public static final String KEY_CURRENT_USER = "currentUser";

    private final UserDataService userDataService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        try {
            // 尝试获取当前登录用户
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                UserDO user = userDataService.getById(userId);
                if (user != null) {
                    RequestContext.set(KEY_CURRENT_USER, user);
                }
            }
        } catch (Exception e) {
            // 获取用户失败不影响请求继续
            log.debug("Failed to get current user for RequestContext: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler,
                               Exception ex) {
        // 请求结束时清理上下文，防止内存泄漏
        RequestContext.clear();
    }
}
