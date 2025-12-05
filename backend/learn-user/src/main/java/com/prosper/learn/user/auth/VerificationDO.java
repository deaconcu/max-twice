package com.prosper.learn.user.auth;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationDO {

    private Long id;

    private String email;

    private String code;

    private LocalDateTime createdAt;

    private Boolean used;

    public VerificationDO() { }

    public VerificationDO(String email, String code) {
        this.email = email;
        this.code = code;
        this.used = false;
    }

}
