package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDO {

    private Long id;

    private Long nodeId;

    private Long creator;

    private Integer type;

    private String content;

    private Integer once;

    private Integer twice;

    private Integer helpful;

    private Integer commentCount;

    private Integer state;

    private Double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    private LocalDateTime cTime;

    private LocalDateTime uTime;

}
