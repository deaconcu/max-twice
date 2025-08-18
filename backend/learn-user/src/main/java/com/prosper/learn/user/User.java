package com.prosper.learn.user;

import com.prosper.learn.common.Aggregate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Data
@Slf4j
public class User implements Aggregate<Integer> {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @Schema(required = true, example = "deacon.cu@gmail.com")
    private String email;

    @Schema(required = true, example = "123456")
    private String password;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String passwordMD5;

    private void check() {
        if (checkEmail(email)) {
            throw new RuntimeException("email is invalid");
        }
        if (checkPassword(password)) {
            throw new RuntimeException("password is invalid");
        }
    }

    private boolean checkEmail(String email) {
        // todo
        //log.info("email checked");
        return true;
    }

    private boolean checkPassword(String password) {
        // todo
        //log.info("password checked");
        return true;
    }

    public void setEmail(String email) {
        checkEmail(email);
        this.email = email;
    }

    public void setPassword(String password) {
        checkPassword(password);
        this.password = password;
        this.passwordMD5 = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
