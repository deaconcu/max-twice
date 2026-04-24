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
     * 获取客户端真实IP地址。
     * <p>
     * 部署架构：Client → Cloudflare → 源站（Hetzner Firewall 只放行 CF IP 段）。
     * 只信任 CF 在回源时添加的 {@code CF-Connecting-IP}（CF 会覆盖客户端伪造的同名 header）；
     * 读不到时兜底到 TCP 对端地址。不读 {@code X-Forwarded-For} / {@code X-Real-IP}，
     * 这两个是客户端可伪造的，任何人打 header 都能顶掉 IP 维度的风控。
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
     * 从 HttpServletRequest 获取客户端真实 IP 地址。见 {@link #getIpAddress()} 的安全说明。
     */
    public static String getIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            String ip = request.getHeader("CF-Connecting-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("从request获取IP地址失败", e);
            return null;
        }
    }
}
