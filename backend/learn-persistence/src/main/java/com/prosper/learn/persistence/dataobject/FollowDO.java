package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowDO {

    private Long followeeId;

    private Long followerId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
