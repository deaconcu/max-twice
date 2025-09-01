package com.prosper.learn.dto.response;

import lombok.Data;

@Data
public class ContentsDTO {

    private Long id;

    private Long userId;

    private String contents;

    private String createdAt;

    private String updatedAt;
}
