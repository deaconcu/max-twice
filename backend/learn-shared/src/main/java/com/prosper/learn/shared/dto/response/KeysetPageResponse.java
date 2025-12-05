package com.prosper.learn.shared.dto.response;

import lombok.Data;

import java.util.List;

/**
 * Keyset分页响应DTO
 */
@Data
public class KeysetPageResponse<T> {

    /**
     * 数据列表
     */
    private List<T> items;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;

    /**
     * 下一页游标信息
     */
    private NextCursor nextCursor;

    @Data
    public static class NextCursor {
        /**
         * 最后一条记录的分数
         */
        private Double lastScore;

        /**
         * 最后一条记录的ID
         */
        private Long lastId;
    }

    /**
     * 创建分页响应
     */
    public static <T> KeysetPageResponse<T> of(List<T> items, boolean hasMore, Double lastScore, Long lastId) {
        KeysetPageResponse<T> response = new KeysetPageResponse<>();
        response.setItems(items);
        response.setHasMore(hasMore);
        
        if (hasMore && lastScore != null && lastId != null) {
            NextCursor cursor = new NextCursor();
            cursor.setLastScore(lastScore);
            cursor.setLastId(lastId);
            response.setNextCursor(cursor);
        }
        
        return response;
    }

}