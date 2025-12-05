package com.prosper.learn.shared.dto.response;

import lombok.Data;

@Data
public class UserDTO {

    private Long id;

    private String name;

    private String password;

    private String phone;

    private String email;

    private Boolean emailValidated;

    private String biography;

    private Byte state;

    private Integer role;  // 新增：用户角色（0=USER, 1=MODERATOR, 2=ADMIN, 3=SUPER_ADMIN）

    private SubscriptionDTO[] subscriptions;

    private boolean isFollowing;

    private String createdAt;

    private String updatedAt;
}
