package com.twicemax.web.v2.config;

import com.twicemax.web.v2.interceptor.ReadOnlyModeInterceptor;
import com.twicemax.web.v2.interceptor.RequestContextInterceptor;
import com.twicemax.web.v2.resolver.CurrentUserArgumentResolver;
import com.twicemax.web.v2.resolver.JsonParamArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置
 * 注册自定义参数解析器和拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JsonParamArgumentResolver jsonParamArgumentResolver;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final ReadOnlyModeInterceptor readOnlyModeInterceptor;
    private final RequestContextInterceptor requestContextInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(jsonParamArgumentResolver);
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册请求上下文拦截器（优先级最高，最先执行）
        registry.addInterceptor(requestContextInterceptor)
                .addPathPatterns("/**")
                .order(0);

        // 注册只读模式拦截器
        // 只拦截写操作（POST/PUT/DELETE/PATCH），GET 请求在拦截器内部会直接放行
        registry.addInterceptor(readOnlyModeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/public/**",                // 公开接口
                        "/auth/**",                  // 登录接口
                        "/config/**",                // 配置接口（读取）
                        "/admin/system/readonly-mode" // 只读模式控制接口
                );
    }
}