package com.prosper.learn.infrastructure.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求上下文
 * 使用 ThreadLocal 存储当前请求的上下文信息
 * 用于在 Service 层或切面中获取上下文数据，无需通过参数传递
 */
public class RequestContext {

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
}
