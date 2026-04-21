package com.twicemax.user.auth;

import lombok.Data;

import java.time.LocalDateTime;

import static com.twicemax.shared.domain.Enums.VerificationType;

@Data
public class VerificationDO {

    private Long id;

    private String email;

    private String code;

    private Byte type;  // 验证码类型：1=注册，2=找回密码，3=修改邮箱

    private LocalDateTime createdAt;

    private Boolean used;

    public VerificationDO() { }

    public VerificationDO(String email, String code) {
        this.email = email;
        this.code = code;
        this.type = VerificationType.REGISTER.value();  // 默认为注册类型
        this.used = false;
    }

    public VerificationDO(String email, String code, Byte type) {
        this.email = email;
        this.code = code;
        this.type = type;
        this.used = false;
    }

}
