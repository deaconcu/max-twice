package com.twicemax.web.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP地址工具类
 */
@Slf4j
public class IpUtils {

    /**
     * 获取客户端真实IP地址
     * 考虑了反向代理的情况（X-Forwarded-For, X-Real-IP）
     *
     * @return IP地址，如果无法获取返回null
     */
    public static String getIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            return getIpAddress(request);
        } catch (Exception e) {
            log.warn("获取IP地址失败", e);
            return null;
        }
    }

    /**
     * 从HttpServletRequest获取客户端真实IP地址
     * 考虑了反向代理的情况（X-Forwarded-For, X-Real-IP）
     *
     * @param request HTTP请求对象
     * @return IP地址，如果无法获取返回null
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            // 考虑反向代理的情况
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // X-Forwarded-For 可能包含多个IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            return ip;
        } catch (Exception e) {
            log.warn("从request获取IP地址失败", e);
            return null;
        }
    }
}
