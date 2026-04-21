package com.twicemax.web.util;

import com.twicemax.infrastructure.datasource.DataSourceContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 国际化消息工具类，用于获取多语言文本
 */
@Component
public class MessageUtils {

    @Autowired
    private MessageSource messageSource;

    /**
     * 根据 DataSourceContextHolder 中的语言获取 Locale
     */
    private Locale getLocale() {
        String lang = DataSourceContextHolder.getLanguage();
        return "en".equalsIgnoreCase(lang) ? Locale.ENGLISH : Locale.SIMPLIFIED_CHINESE;
    }

    // 获取国际化消息
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, getLocale());
    }

    // 获取带参数的国际化消息
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, getLocale());
    }

    // 获取国际化消息，支持默认值
    public String getMessage(String code, String defaultMessage) {
        return messageSource.getMessage(code, null, defaultMessage, getLocale());
    }

    // 获取带参数的国际化消息，支持默认值
    public String getMessage(String code, Object[] args, String defaultMessage) {
        return messageSource.getMessage(code, args, defaultMessage, getLocale());
    }
}