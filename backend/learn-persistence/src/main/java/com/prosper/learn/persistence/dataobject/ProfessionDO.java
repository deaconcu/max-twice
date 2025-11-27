package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfessionDO {

    private Long id;

    private String name;

    private String description;

    private String price;

    private String skills;

    private Integer mainCategory;

    private Integer subCategory;

    private Byte state; // 改为 tinyint 类型，支持 SUBMITTED=0, APPROVED=1, REJECTED=2

    private String reason; // 拒绝原因，默认为空字符串

    private String icon; // 图标字段

    private Long creatorId; // 创建者 ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
