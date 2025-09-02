package com.prosper.learn.dto.response;

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

    private String createdAt;

    private String updatedAt;
}
