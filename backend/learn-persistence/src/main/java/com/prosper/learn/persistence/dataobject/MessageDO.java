package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDO {

    private Long id;

    private Long senderId;

    private Long receiverId;

    private String content;

    private Integer type;

    private Integer category;  // 新增字段：消息分类 1=互动消息, 2=系统消息, 3=私信

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}
