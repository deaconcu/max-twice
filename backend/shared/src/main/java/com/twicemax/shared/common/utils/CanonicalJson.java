package com.twicemax.shared.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Canonical JSON：把 JSON 字符串归一化成"同一逻辑数据 → 唯一字符串"的形式，便于做 hash 去重。
 * <p>
 * 归一化规则：
 * <ul>
 *   <li>对象（object）的字段按 key 字典序排序；</li>
 *   <li>数组（array）保持原顺序——roadmap 的 trunk/children 顺序有业务含义，不能动；</li>
 *   <li>无多余空白（紧凑输出）。</li>
 * </ul>
 * 不做的事：数字字面量归一（如 1.0 与 1 不会被合并）、字符串 NFC 归一。
 */
public final class CanonicalJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CanonicalJson() {}

    /**
     * 把任意合法 JSON 字符串归一化输出。
     *
     * @throws IllegalArgumentException 输入不是合法 JSON。
     */
    public static String canonicalize(String json) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode sorted = sortKeys(node);
            return MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid json", e);
        }
    }

    /** Canonical JSON 的 SHA-256 hex（小写）。 */
    public static String hash(String json) {
        return Utils.hashSHA(canonicalize(json));
    }

    /** 递归重建：object 排序、array 保序、其他类型原样。 */
    private static JsonNode sortKeys(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode arr = JsonNodeFactory.instance.arrayNode(node.size());
            for (JsonNode child : node) {
                arr.add(sortKeys(child));
            }
            return arr;
        }
        if (node.isObject()) {
            List<String> fields = new ArrayList<>();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                fields.add(it.next());
            }
            Collections.sort(fields);
            ObjectNode obj = JsonNodeFactory.instance.objectNode();
            for (String name : fields) {
                obj.set(name, sortKeys(node.get(name)));
            }
            return obj;
        }
        return node;
    }
}
