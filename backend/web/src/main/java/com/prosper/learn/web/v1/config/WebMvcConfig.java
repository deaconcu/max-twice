package com.prosper.learn.web.v1.config;

import com.prosper.learn.web.v1.interceptor.ReadOnlyModeInterceptor;
import com.prosper.learn.web.v1.interceptor.RequestContextInterceptor;
import com.prosper.learn.web.v1.resolver.CurrentUserArgumentResolver;
import com.prosper.learn.web.v1.resolver.JsonParamArgumentResolver;
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
                .addPathPatterns("/v1/**")
                .order(0);

        // 注册只读模式拦截器
        // 只拦截写操作（POST/PUT/DELETE/PATCH），GET 请求在拦截器内部会直接放行
        registry.addInterceptor(readOnlyModeInterceptor)
                .addPathPatterns("/v1/**")  // 只拦截 v1 API
                .excludePathPatterns(
                        "/v1/public/**",           // 公开接口
                        "/v1/auth/login",          // 登录接口
                        "/v1/auth/register",       // 注册接口
                        "/v1/auth/validate-email", // 邮箱验证
                        "/v1/config/**",           // 配置接口（读取）
                        "/v1/admin/system/readonly-mode"  // 只读模式控制接口（已在拦截器内部排除，这里再排除一次更清晰）
                );
    }
}