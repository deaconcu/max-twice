package com.twicemax.application.dto.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.domain.exception.StatusCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用游标，支持单字段（ID）和复合字段（score + ID）两种模式。
 *
 * <p>encode 后为 base64url(JSON)，对前端不透明，直接透传即可。
 *
 * <ul>
 *   <li>仅 ID：{@code Cursor.of(id).encode()} → base64url({"id":98765})</li>
 *   <li>score + ID：{@code Cursor.of(score, id).encode()} → base64url({"score":10.5,"id":98765})</li>
 * </ul>
 */
public final class Cursor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Double score;
    private final Long id;

    private Cursor(Double score, Long id) {
        this.score = score;
        this.id = id;
    }

    /** 构造仅含 ID 的游标 */
    public static Cursor of(Long id) {
        return new Cursor(null, id);
    }

    /** 构造 score + ID 的复合游标 */
    public static Cursor of(Double score, Long id) {
        return new Cursor(score, id);
    }

    public Double score() {
        return score;
    }

    public Long id() {
        return id;
    }

    /**
     * 解码 base64url 游标字符串。
     *
     * @param cursor 游标字符串，为 null 时返回空游标（两字段均为 null，表示首页）
     */
    public static Cursor decode(String cursor) {
        if (cursor == null) {
            return new Cursor(null, null);
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            Map<String, Object> map = MAPPER.readValue(
                    new String(bytes, StandardCharsets.UTF_8),
                    new TypeReference<>() {});
            Double score = map.get("score") != null
                    ? ((Number) map.get("score")).doubleValue() : null;
            Long id = map.get("id") != null
                    ? ((Number) map.get("id")).longValue() : null;
            return new Cursor(score, id);
        } catch (Exception e) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的游标参数");
        }
    }

    /**
     * 编码为 base64url 字符串。score 为 null 时不序列化该字段。
     */
    public String encode() {
        try {
            Map<String, Object> map = new HashMap<>();
            if (score != null) {
                map.put("score", score);
            }
            map.put("id", id);
            String json = MAPPER.writeValueAsString(map);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("游标序列化失败", e);
        }
    }
}
