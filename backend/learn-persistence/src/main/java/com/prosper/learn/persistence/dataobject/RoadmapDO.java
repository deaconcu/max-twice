package com.prosper.learn.persistence.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoadmapDO {

    private int id;

    private String content;

    private String contentHash;

    private int professionId;

    private int creatorId;

    private String description;

    private int vote;

    private int comment;

    private double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
