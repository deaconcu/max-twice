package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseTocDO {

    private String hash;

    private String toc;

    private Integer refCount;

    private LocalDateTime createdAt;

    public CourseTocDO() {}

    public CourseTocDO(String hash, String toc) {
        this.hash = hash;
        this.toc = toc;
    }

}
