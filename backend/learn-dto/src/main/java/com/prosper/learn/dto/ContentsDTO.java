package com.prosper.learn.dto;

import lombok.Data;

@Data
public class ContentsDTO {

    private int id;

    private int userId;

    private String contents;

    private String createdAt;

    private String updatedAt;
}
