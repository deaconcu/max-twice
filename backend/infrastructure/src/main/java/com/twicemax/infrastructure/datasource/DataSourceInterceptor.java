package com.twicemax.infrastructure.datasource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 数据源拦截器
 * 从请求头读取语言标识，设置到 ThreadLocal 中用于数据源路由
 */
@Slf4j
@Component
public class DataSourceInterceptor implements HandlerInterceptor {

    /**
     * 请求头中的语言标识
     */
    public static final String HEADER_SITE_LANG = "X-Site-Lang";

    /**
     * 支持的语言列表
     */
    private static final String[] SUPPORTED_LANGUAGES = {"zh", "en"};

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头获取语言
        String lang = request.getHeader(HEADER_SITE_LANG);

        // 验证语言是否支持
        if (lang == null || !isSupportedLanguage(lang)) {
            lang = DataSourceContextHolder.DEFAULT_LANGUAGE;
        }

        // 设置到 ThreadLocal
        DataSourceContextHolder.setLanguage(lang);

        log.debug("数据源路由: lang={}, uri={}", lang, request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理 ThreadLocal，防止内存泄漏
        DataSourceContextHolder.clear();
    }

    /**
     * 检查语言是否支持
     */
    private boolean isSupportedLanguage(String lang) {
        for (String supported : SUPPORTED_LANGUAGES) {
            if (supported.equalsIgnoreCase(lang)) {
                return true;
            }
        }
        return false;
    }
}
