package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubcourseDO {

    private int id;

    private String name;

    private String description;

    private int courseId;

    private int creator;

    private LocalDateTime cTime;

    private LocalDateTime uTime;
}
