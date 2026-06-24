package com.twicemax.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Accept-Language 解析工具，用来在用户首次建号时决定默认 locale。
 * <p>
 * 规则：取 header 的第一段 language-range，开头是 {@code zh} 视为 {@code "zh"}，
 * 其余一律 {@code "en"}。不支持 q-value 排序（海外产品优先保证英文兜底，避免歧义）。
 */
public final class AcceptLanguageUtils {

    public static final String LOCALE_ZH = "zh";
    public static final String LOCALE_EN = "en";
    public static final String DEFAULT_LOCALE = LOCALE_EN;

    private AcceptLanguageUtils() {}

    /**
     * 从当前请求解析 locale。拿不到请求时返回默认值 {@code "en"}。
     */
    public static String detectLocale() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return DEFAULT_LOCALE;
            }
            return detectLocale(attributes.getRequest());
        } catch (Exception e) {
            return DEFAULT_LOCALE;
        }
    }

    public static String detectLocale(HttpServletRequest request) {
        if (request == null) return DEFAULT_LOCALE;
        return parse(request.getHeader("Accept-Language"));
    }

    /**
     * 校验是否是合法的 locale（用于 PUT /account/locale 入参）。
     */
    public static boolean isSupported(String locale) {
        return LOCALE_ZH.equals(locale) || LOCALE_EN.equals(locale);
    }

    static String parse(String header) {
        if (header == null || header.isBlank()) return DEFAULT_LOCALE;
        // 取第一段：zh-CN,zh;q=0.9,en;q=0.8 → "zh-CN"
        String first = header.split(",", 2)[0].trim();
        // 去掉 q-value：zh-CN;q=0.9 → "zh-CN"
        int semi = first.indexOf(';');
        if (semi >= 0) first = first.substring(0, semi).trim();
        if (first.isEmpty()) return DEFAULT_LOCALE;
        String lower = first.toLowerCase();
        if (lower.startsWith(LOCALE_ZH)) return LOCALE_ZH;
        return LOCALE_EN;
    }
}
