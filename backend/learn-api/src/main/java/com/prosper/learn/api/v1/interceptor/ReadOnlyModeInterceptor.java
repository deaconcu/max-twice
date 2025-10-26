package com.prosper.learn.api.v1.interceptor;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.data.SystemDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 只读模式拦截器
 * 当系统处于只读模式时，拦截所有写操作（POST/PUT/DELETE/PATCH）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReadOnlyModeInterceptor implements HandlerInterceptor {

    private final SystemDataService systemDataService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        // 排除只读模式控制接口本身（允许关闭只读模式）
        String uri = request.getRequestURI();
        if (uri.endsWith("/system/readonly-mode")) {
            return true;
        }

        // 只拦截写操作
        String method = request.getMethod();
        if (isWriteMethod(method)) {
            // 检查是否只读模式
            if (systemDataService.isReadOnlyMode()) {
                log.warn("只读模式拦截: {} {}", method, request.getRequestURI());
                throw ErrorCode.SYSTEM_READONLY_MODE.exception();
            }
        }
        return true;
    }

    /**
     * 判断是否为写操作
     */
    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "DELETE".equalsIgnoreCase(method)
            || "PATCH".equalsIgnoreCase(method);
    }
}
