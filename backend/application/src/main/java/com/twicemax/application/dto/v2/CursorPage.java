package com.twicemax.application.dto.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * v2 游标分页响应体。
 * <p>
 * 形如：
 * <pre>
 * {
 *   "items": [...],
 *   "hasMore": true,
 *   "nextCursor": 120
 * }
 * </pre>
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPage<T> {

    private List<T> items;

    private boolean hasMore;

    /**
     * 下一页游标。可以是 Long、String、或复合对象（推荐 base64 编码字符串）。
     * 当 hasMore = false 时为 null，Jackson 全局策略下仍会输出 "nextCursor": null（v2 规范要求）。
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Object nextCursor;

    public static <T> CursorPage<T> of(List<T> items, boolean hasMore, Object nextCursor) {
        return new CursorPage<>(items, hasMore, nextCursor);
    }

    public static <T> CursorPage<T> empty() {
        return new CursorPage<>(List.of(), false, null);
    }
}
