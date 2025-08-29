package com.prosper.learn.api.v1.dto;

import lombok.Data;
import java.util.List;

/**
 * 分页响应格式
 * 
 * @param <T> 数据类型
 */
@Data
public class PagedResponse<T> {
    
    private List<T> content;        // 数据列表
    private Integer page;           // 当前页码（从0开始）
    private Integer size;           // 页面大小
    private Long totalElements;     // 总记录数
    private Integer totalPages;     // 总页数
    private Boolean hasNext;        // 是否有下一页
    private Boolean hasPrevious;    // 是否有上一页
    private Boolean first;          // 是否首页
    private Boolean last;           // 是否最后一页
    
    public PagedResponse() {}
    
    public PagedResponse(List<T> content, Integer page, Integer size, Long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
        this.first = page == 0;
        this.last = page == totalPages - 1;
    }
    
    /**
     * 创建分页响应
     */
    public static <T> PagedResponse<T> of(List<T> content, Integer page, Integer size, Long totalElements) {
        return new PagedResponse<>(content, page, size, totalElements);
    }
    
    /**
     * 空的分页响应
     */
    public static <T> PagedResponse<T> empty(Integer page, Integer size) {
        return new PagedResponse<>(List.of(), page, size, 0L);
    }
}