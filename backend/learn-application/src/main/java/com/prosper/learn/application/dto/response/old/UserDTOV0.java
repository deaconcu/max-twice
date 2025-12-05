package com.prosper.learn.application.dto.response.old;

import lombok.Data;

@Data
public class UserDTOV0 {

    private Long id;

    private String name;

    private String password;

    private String phone;

    private String email;

    private Boolean emailValidated;

    private String biography;

    private String createdAt;

    private String updatedAt;
}
