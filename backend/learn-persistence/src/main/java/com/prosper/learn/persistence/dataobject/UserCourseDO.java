package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCourseDO {

    private Long id;

    private Long userId;

    private Long courseId;

    private Integer progressPercent;

    private Byte state; // 原status字段重命名为state，改为 tinyint 类型，支持 NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}