package com.prosper.learn.shared.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.exception.BusinessException;
import com.prosper.learn.shared.domain.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Utils {

    private static ObjectMapper objectMapper;

    public Utils(ObjectMapper objectMapper) {
        Utils.objectMapper = objectMapper;
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");
    private static final LocalDateTime now = LocalDateTime.now();

    public static String getTimeString() {
        return dtf.format(now);
    }

    public static String getTimeString(LocalDateTime time) {
        return dtf.format(time);
    }

    public static LocalDateTime getLocalDateTime() {
        return now;
    }

    public static Pair<Long, JsonNode> getNodeByPath(JsonNode rootNode, String path) {
        String[] keys = path.split("\\-");  // 使用 "-" 分割路径
        JsonNode currentNode = rootNode;

        int index = 0;
        long nodeId = 0;
        for (String key : keys) {
            if (index == 0) {
                currentNode = currentNode.get(Integer.parseInt(key) - 1);
            } else {
                if (currentNode != null && currentNode.has(key)) {
                    nodeId = Integer.parseInt(key);
                    currentNode = currentNode.get(key);
                } else {
                    return null;
                }
            }
            index ++;
        }
        return new Pair<>(nodeId, currentNode);
    }

    public static String md5(String input) {
        try {
            // 获取 MD5 消息摘要实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 计算摘要，返回字节数组
            byte[] messageDigest = md.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // 补零
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    public static String hashSHA(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得json串的所有key
     */
    public static void collectKeys(JsonNode node, Set<Long> keys) {
        if (node.isObject()) {  // 如果节点是对象
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.equals("+") || fieldName.equals("^")) continue;
                keys.add(Long.parseLong(fieldName));
                collectKeys(node.get(fieldName), keys);  // 递归遍历子节点
            }
        } else if (node.isArray()) {  // 如果节点是数组
            for (JsonNode arrayItem : node) {
                collectKeys(arrayItem, keys);  // 递归遍历数组的每一项
            }
        }
    }

    /**
     * 二元组
     */
    public record Pair<L, R>(L left, R right) {};

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
     * @throws BusinessException 如果状态转换不合法
     *
     * 支持的状态转换:
     * - approve: SUBMITTED → PUBLISHED
     * - reject: SUBMITTED → REJECTED
     * - remove: PUBLISHED → REJECTED (下架已发布内容)
     * - ban: 任何状态 → BANNED
     * - restore: REJECTED/BANNED → PUBLISHED (恢复被拒绝或被封禁的内容)
     */
    public static void validateStateTransition(Byte currentState, Enums.ContentState targetState) {
        if (currentState == null || targetState == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("状态不能为空");
        }

        Enums.ContentState current = Enums.ContentState.getByValue(currentState);
        if (current == null) {
            throw ErrorCode.INVALID_PARAMETER.exception("无效的当前状态: " + currentState);
        }

        boolean isValid = switch (targetState) {
            case PUBLISHED ->
                // approve: SUBMITTED → PUBLISHED
                // restore: REJECTED/BANNED → PUBLISHED
                current == Enums.ContentState.SUBMITTED
                    || current == Enums.ContentState.REJECTED
                    || current == Enums.ContentState.BANNED;
            case REJECTED ->
                // reject: SUBMITTED → REJECTED
                // remove: PUBLISHED → REJECTED
                current == Enums.ContentState.SUBMITTED
                    || current == Enums.ContentState.PUBLISHED;
            case BANNED ->
                // ban: 任何状态 → BANNED (除了已经是 BANNED)
                current != Enums.ContentState.BANNED;
            case DRAFT, SUBMITTED ->
                // 不允许转回 DRAFT 或 SUBMITTED 状态
                false;
        };

        if (!isValid) {
            throw ErrorCode.INVALID_OPERATION.exception(
                    String.format("不允许从 %s 状态转换到 %s 状态", current, targetState)
            );
        }
    }
}
