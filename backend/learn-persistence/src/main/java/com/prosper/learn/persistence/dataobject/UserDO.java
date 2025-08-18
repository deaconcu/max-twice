package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDO {

    private int id;

    private String password;

    private String email;

    private String phone;

    private String name;

    private boolean emailValidated;

    private String biography;

    private LocalDateTime msgReadTime;

    private LocalDateTime cTime;

    private LocalDateTime uTime;
}
