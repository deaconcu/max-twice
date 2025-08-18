package com.prosper.learn.front.web;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginInterceptor implements WebMvcConfigurer {

    @Bean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor();  // 默认的 Sa-Token 拦截器
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/login");  // 排除不需要验证的路径
    }

}
