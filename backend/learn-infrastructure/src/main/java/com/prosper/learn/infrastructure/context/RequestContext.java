package com.prosper.learn.infrastructure.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求上下文
 * 使用 ThreadLocal 存储当前请求的上下文信息
 * 用于在 Service 层或切面中获取上下文数据，无需通过参数传递
 */
public class RequestContext {

    // 常用 key 定义
    public static final String KEY_CURRENT_USER = "currentUser";
    public static final String KEY_IP_ADDRESS = "ipAddress";

    private static final ThreadLocal<Map<String, Object>> context = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置属性
     */
    public static void set(String key, Object value) {
        context.get().put(key, value);
    }

    /**
     * 获取属性
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) context.get().get(key);
    }

    /**
     * 移除属性
     */
    public static void remove(String key) {
        context.get().remove(key);
    }

    /**
     * 清除所有上下文数据（请求结束时必须调用，防止内存泄漏）
     */
    public static void clear() {
        context.remove();
    }

    // ========== 便捷方法 ==========

    /**
     * 获取当前用户
     * @return 当前用户对象（类型由调用方确定）
     */
    public static <T> T getCurrentUser() {
        return get(KEY_CURRENT_USER);
    }

    /**
     * 设置当前用户
     */
    public static void setCurrentUser(Object user) {
        set(KEY_CURRENT_USER, user);
    }

    /**
     * 获取客户端 IP 地址
     */
    public static String getIpAddress() {
        return get(KEY_IP_ADDRESS);
    }

    /**
     * 设置客户端 IP 地址
     */
    public static void setIpAddress(String ipAddress) {
        set(KEY_IP_ADDRESS, ipAddress);
    }
}
