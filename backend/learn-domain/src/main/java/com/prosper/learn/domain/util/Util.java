package com.prosper.learn.domain.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.persistence.dataobject.UserDO;
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
}
