package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostStatsDO {

    private Long id;

    private String type;

    private Long objectId;

    private String stats;  // JSON格式存储点赞数据，格式：{"1-1":{"twice": 3, "helpful": 2, "views": 100, "comments": 8}, ...}

    private Integer statYear;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
