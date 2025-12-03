package com.prosper.learn.business.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Util {

    private static ObjectMapper objectMapper;

    public Util(ObjectMapper objectMapper) {
        Util.objectMapper = objectMapper;
    }

    public static String toJson(Object object) {
        try{
            return objectMapper.writeValueAsString(object);
        } catch ( Exception e ) {
            throw new RuntimeException("Parse json failed", e);
        }
    }

    public static Map<String, Object> readValueToMap(String json) {
        try{
            return objectMapper.readValue(json, Map.class);
        } catch ( Exception e ) {
            throw new RuntimeException("Parse json failed", e);
        }
    }

    /**
     * 安全地从 Map 中获取 Long 值
     */
    public static Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert value '" + value + "' to Long for key: " + key);
            }
        }
        throw new IllegalArgumentException("Cannot convert value of type " + value.getClass().getSimpleName() + " to Long for key: " + key);
    }

    /**
     * 安全地从 Map 中获取 Integer 值
     */
    public static Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert value '" + value + "' to Integer for key: " + key);
            }
        }
        throw new IllegalArgumentException("Cannot convert value of type " + value.getClass().getSimpleName() + " to Integer for key: " + key);
    }

    public static List<Long> getIds(Collection<? extends Object> dtos, Function<Object, Long> userIdExtractor) {
        List<Long> ids = dtos.stream()
                .map(userIdExtractor)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        return ids;
    }

    /**
     * 安全地从 Map 中获取 String 值
     */
    public static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 去除文本中的 Markdown 和 HTML 格式，返回纯文本
     * @param text 包含格式的文本
     * @return 纯文本
     */
    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                // 移除 HTML 标签
                .replaceAll("<[^>]*>", "")
                // 移除 Markdown 标题标记
                .replaceAll("#{1,6}\\s+", "")
                // 移除 Markdown 粗体和斜体
                .replaceAll("(\\*\\*|__)(.*?)\\1", "$2")
                .replaceAll("(\\*|_)(.*?)\\1", "$2")
                // 移除 Markdown 链接，保留链接文本
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
                // 移除 Markdown 图片
                .replaceAll("!\\[([^\\]]*)\\]\\([^\\)]+\\)", "")
                // 移除 Markdown 代码块标记
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]+)`", "$1")
                // 移除 Markdown 引用标记
                .replaceAll("(?m)^>\\s+", "")
                // 移除 Markdown 列表标记
                .replaceAll("(?m)^[\\*\\-\\+]\\s+", "")
                .replaceAll("(?m)^\\d+\\.\\s+", "")
                // 移除多余的空白字符
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 验证内容状态转换是否合法
     * @param currentState 当前状态
     * @param targetState 目标状态
     * @throws com.prosper.learn.common.exception.BusinessException 如果状态转换不合法
     */
    public static void validateStateTransition(Byte currentState, Enums.ContentState targetState) {
        if (currentState == null || targetState == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("状态不能为空");
        }

        Enums.ContentState current = Enums.ContentState.getByValue(currentState.intValue());
        if (current == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的当前状态: " + currentState);
        }

        boolean isValid = switch (targetState) {
            case PUBLISHED -> current == Enums.ContentState.SUBMITTED
                           || current == Enums.ContentState.REJECTED
                           || current == Enums.ContentState.BANNED;  // 待审核/已拒绝/已封禁 都可以发布
            case REJECTED -> current == Enums.ContentState.SUBMITTED
                          || current == Enums.ContentState.BANNED;   // 待审核/已封禁 可以拒绝
            case BANNED -> current != Enums.ContentState.BANNED;     // 除了已封禁，其他状态都可以封禁
            case SUBMITTED -> false;  // 不允许转回待审核状态
        };

        if (!isValid) {
            throw ErrorCode.INVALID_OPERATION.exception(
                String.format("不允许从 %s 状态转换到 %s 状态", current, targetState)
            );
        }
    }
}