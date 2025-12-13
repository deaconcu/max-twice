package com.prosper.learn.content.post;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDO {

    private Long id;

    private Long nodeId;

    private Long creatorId;

    private Integer type;

    private String content;

    private Integer once;

    private Byte state;

    private String reason;  // 拒绝/封禁原因

    private Double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
