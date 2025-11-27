package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.prosper.learn.common.Utils;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VerificationDO {

    private Long id;

    private String email;

    private String code;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Boolean used;

    public VerificationDO() { }

    public VerificationDO(String email, String code) {
        this.email = email;
        this.code = code;
        this.used = false;
    }

}
