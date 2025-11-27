package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseTocDO {

    private String hash;

    private String toc;

    private Integer refCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public CourseTocDO() {}

    public CourseTocDO(String hash, String toc) {
        this.hash = hash;
        this.toc = toc;
        this.refCount = 0;
    }

}
