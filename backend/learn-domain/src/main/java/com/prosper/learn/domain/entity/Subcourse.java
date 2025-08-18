package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import lombok.Data;

import java.util.Date;

@Data
public class Subcourse implements Aggregate<Integer> {

    private Integer id;

    private String name;

    private String description;

    private int courseId;

    private int userId;

    private Date createTime;

    private Date updateTime;

    public Subcourse(String name, String description, int courseId, int userId) {
        this.name = name;
        this.description = description;
        this.courseId = courseId;
        this.userId = userId;
    }
}
