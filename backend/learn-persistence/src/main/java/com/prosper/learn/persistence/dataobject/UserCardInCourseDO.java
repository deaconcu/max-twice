package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCardInCourseDO {

    private Long id;

    private Long userId;

    private Long cardId;

    private Long deckId;  // 冗余字段：记忆卡片组ID，用于快速过滤被屏蔽的卡片组

    private Long courseId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}