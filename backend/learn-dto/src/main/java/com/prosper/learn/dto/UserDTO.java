package com.prosper.learn.dto;

import lombok.Data;

@Data
public class UserDTO {

    private int id;

    private String name;

    private String password;

    private String phone;

    private String email;

    private boolean emailValidated;

    private String biography;



    private String cTime;

    private String uTime;
}
