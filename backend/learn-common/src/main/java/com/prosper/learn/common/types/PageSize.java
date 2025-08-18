package com.prosper.learn.common.types;

import lombok.Getter;

@Getter
public class PageSize {

    private static final int DEFAULT_LIMIT = 20;

    private int size;

    public PageSize(int size) {
        this(size, DEFAULT_LIMIT);
    }

    public PageSize(int size, int limit) {
        if (size < 1 || size > limit) {
            this.size = limit;
        } else {
            this.size = size;
        }
    }

    public int offset(int page) {
        return (page - 1) * size;
    }
}