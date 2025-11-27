package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDO {

    private Long id;

    private String name;

    private String description;

    private Long creatorId;

    private Long rootNodeId;

    private Long parentCourseId;

    private Byte state; // 改为 tinyint 类型，支持 SUBMITTED=0, APPROVED=1, REJECTED=2

    private Integer mainCategory; // 新增主分类字段

    private Integer subCategory; // 新增子分类字段

    private String reason; // 拒绝原因，默认为空字符串

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
