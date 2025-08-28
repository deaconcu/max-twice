package com.prosper.learn.dto;

import lombok.Data;

@Data
public class FolloweeDTO {

    private Long id;

    private String name;

    private String biography;

    private String createdAt;
}
