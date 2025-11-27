package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostStatsDO {

    private Long id;

    private Integer objectType; // 使用 ObjectType 枚举值（post=1, roadmap=4）

    private Long objectId;

    private String stats;  // JSON格式存储点赞数据，格式：{"1-1":{"twice": 3, "helpful": 2, "views": 100, "comments": 8}, ...}

    private Integer statYear;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
