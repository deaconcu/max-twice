package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDO {

    private int id;

    private int nodeId;

    private int creator;

    private int type;

    private String content;

    private int once;

    private int twice;

    private int helpful;

    private int commentCount;

    private int state;

    private double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    private LocalDateTime cTime;

    private LocalDateTime uTime;

}
