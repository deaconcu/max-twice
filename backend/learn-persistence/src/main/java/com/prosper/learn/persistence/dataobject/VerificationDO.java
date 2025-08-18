package com.prosper.learn.persistence.dataobject;

import com.prosper.learn.common.Utils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationDO {

    public VerificationDO(){}

    public VerificationDO(String email, String code) {
        this.email = email;
        this.code = code;
        this.cTime = Utils.getLocalDateTime();
        this.used = false;
    }

    private int id;
    private String email;
    private String code;
    private LocalDateTime cTime;
    private boolean used;
}
