package com.prosper.learn.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

public class Utils {

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
}
