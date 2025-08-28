package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDO {

    private Long id;

    private String password;

    private String email;

    private String phone;

    private String name;

    private Boolean emailValidated;

    private String biography;

    private LocalDateTime msgReadTime;

    private LocalDateTime cTime;

    private LocalDateTime uTime;
}
