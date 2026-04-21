package com.twicemax.content.toc;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeTocDO {

    private String hash;

    private String toc;

    private Integer refCount;

    private LocalDateTime createdAt;

    public NodeTocDO() {}

    public NodeTocDO(String hash, String toc) {
        this.hash = hash;
        this.toc = toc;
        this.refCount = 0;
    }

}
