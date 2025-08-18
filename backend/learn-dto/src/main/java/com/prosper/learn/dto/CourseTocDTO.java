package com.prosper.learn.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CourseTocDTO {

    private String contents;

    // 课程id和课程名字的map
    private Map<Integer, String> names;

    public CourseTocDTO(String contents, Map<Integer, String> names) {
        this.contents = contents;
        this.names = names;
    }

}
