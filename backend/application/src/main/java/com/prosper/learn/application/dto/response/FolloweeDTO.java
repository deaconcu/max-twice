package com.prosper.learn.application.dto.response;

import lombok.Data;

@Data
public class FolloweeDTO {

    private Long id;  // 关注记录的ID，用于分页

    private Long userId;  // 被关注用户的ID

    private String name;

    private String biography;

    private String avatar;

    private String createdAt;
}
