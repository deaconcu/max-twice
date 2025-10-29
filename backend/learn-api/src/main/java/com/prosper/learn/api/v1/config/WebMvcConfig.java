package com.prosper.learn.api.v1.config;

import com.prosper.learn.api.v1.interceptor.ReadOnlyModeInterceptor;
import com.prosper.learn.api.v1.resolver.CurrentUserArgumentResolver;
import com.prosper.learn.api.v1.resolver.JsonParamArgumentResolver;
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

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(jsonParamArgumentResolver);
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册只读模式拦截器
        registry.addInterceptor(readOnlyModeInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 API 请求
                .excludePathPatterns(
                        "/api/v1/public/**",  // 排除公开接口
                        "/api/v1/login",      // 排除登录接口
                        "/api/v1/register"    // 排除注册接口
                );
    }
}