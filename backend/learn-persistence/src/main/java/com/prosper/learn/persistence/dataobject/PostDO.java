package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class PostDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long nodeId;

    private Long creatorId;

    private Integer type;

    private String content;

    private Integer once;

    private Integer twice;

    private Integer helpful;

    private Integer commentCount;

    private Integer viewCount;

    private Byte state;

    private String reason;  // 拒绝/封禁原因

    private Double score;  // 计算出的排序分数

    private LocalDateTime scoreCalculatedAt;  // 分数计算时间

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}
