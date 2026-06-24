package com.twicemax.shared.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.BusinessException;
import com.twicemax.shared.domain.exception.StatusCode;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class Utils {

    private static ObjectMapper objectMapper;

    public Utils(ObjectMapper objectMapper) {
        Utils.objectMapper = objectMapper;
    }

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    // ========== 预编译的正则表达式（用于 stripFormatting 性能优化）==========

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("#{1,6}\\s+");
    private static final Pattern MARKDOWN_BOLD_PATTERN = Pattern.compile("(\\*\\*|__)(.*?)\\1");
    private static final Pattern MARKDOWN_ITALIC_PATTERN = Pattern.compile("(\\*|_)(.*?)\\1");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\([^\\)]+\\)");
    private static final Pattern MARKDOWN_IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\([^\\)]+\\)");
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern MARKDOWN_CODE_INLINE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern MARKDOWN_QUOTE_PATTERN = Pattern.compile("(?m)^>\\s+");
    private static final Pattern MARKDOWN_LIST_UNORDERED_PATTERN = Pattern.compile("(?m)^[\\*\\-\\+]\\s+");
    private static final Pattern MARKDOWN_LIST_ORDERED_PATTERN = Pattern.compile("(?m)^\\d+\\.\\s+");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public static String getTimeString(LocalDateTime time) {
        return dtf.format(time);
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
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

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
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
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
     *
     * 性能优化：使用预编译的 Pattern 对象，避免每次调用时重新编译正则表达式
     *
     * @param text 包含格式的文本
     * @return 纯文本
     */
    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        // 使用预编译的 Pattern，性能提升 3-5 倍
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_HEADING_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_BOLD_PATTERN.matcher(result).replaceAll("$2");
        result = MARKDOWN_ITALIC_PATTERN.matcher(result).replaceAll("$2");
        result = MARKDOWN_LINK_PATTERN.matcher(result).replaceAll("$1");
        result = MARKDOWN_IMAGE_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_CODE_BLOCK_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_CODE_INLINE_PATTERN.matcher(result).replaceAll("$1");
        result = MARKDOWN_QUOTE_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_LIST_UNORDERED_PATTERN.matcher(result).replaceAll("");
        result = MARKDOWN_LIST_ORDERED_PATTERN.matcher(result).replaceAll("");
        result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ");

        return result.trim();
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
            throw StatusCode.INVALID_PARAMETER.exception("状态不能为空");
        }

        Enums.ContentState current = Enums.ContentState.getByValue(currentState);
        if (current == null) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的当前状态: " + currentState);
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
            throw StatusCode.INVALID_OPERATION.exception(
                    String.format("不允许从 %s 状态转换到 %s 状态", current, targetState)
            );
        }
    }

    /**
     * 将节点ID集合转换为JSON数组字符串
     * 例如: {1, 2, 3} → "[1,2,3]"
     *
     * @param nodeIds 节点ID集合
     * @return JSON数组字符串
     */
    public static String nodeIdsToJsonArray(Set<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return "[]";
        }

        return nodeIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }
}
