package com.twicemax.infrastructure.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 数据源上下文持有者
 * 使用 ThreadLocal 存储当前请求的语言标识，用于动态数据源路由
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<String> LANGUAGE = new ThreadLocal<>();

    /**
     * 默认语言
     */
    public static final String DEFAULT_LANGUAGE = "zh";

    /**
     * 支持的所有语言列表
     */
    public static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("zh", "en");

    /**
     * 设置当前线程的语言
     */
    public static void setLanguage(String language) {
        LANGUAGE.set(language);
    }

    /**
     * 获取当前线程的语言
     */
    public static String getLanguage() {
        String lang = LANGUAGE.get();
        return lang != null ? lang : DEFAULT_LANGUAGE;
    }

    /**
     * 清除当前线程的语言设置
     * 必须在请求结束时调用，防止内存泄漏
     */
    public static void clear() {
        LANGUAGE.remove();
    }

    /**
     * 为每种语言执行操作（用于定时任务）
     * 执行完毕后会清除语言上下文
     *
     * @param action 要执行的操作，参数为当前语言
     */
    public static void forEachLanguage(Consumer<String> action) {
        for (String lang : SUPPORTED_LANGUAGES) {
            try {
                setLanguage(lang);
                action.accept(lang);
            } finally {
                clear();
            }
        }
    }

    /**
     * 在指定语言上下文中执行操作
     * 执行完毕后会清除语言上下文
     *
     * @param language 语言
     * @param action 要执行的操作
     */
    public static void runWithLanguage(String language, Runnable action) {
        try {
            setLanguage(language);
            action.run();
        } finally {
            clear();
        }
    }
}
