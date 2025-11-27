package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStatsDO {

    private Long userId;

    private String stats;  // JSON格式存储统计数据，格式：{"1-1":{"views": 10, "twice": 3, "helpful": 2, "comments": 5}, ...}

    private Integer statYear;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}