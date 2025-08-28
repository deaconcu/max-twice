package com.prosper.learn.domain.entity;

import com.prosper.learn.common.Aggregate;
import com.prosper.learn.common.Enums;
import lombok.Data;

import java.util.Date;

@Data
public class Course implements Aggregate<Integer> {

    private Integer id;

    private String name;

    private String description;

    private int userId;

    private Enums.CourseState state;

    private Date createTime;

    private Date updateTime;

    public Course(String name, String description, int userId) {
        this.name = name;
        this.description = description;
        this.userId = userId;
        state = Enums.CourseState.SUBMITTED;
    }
}
