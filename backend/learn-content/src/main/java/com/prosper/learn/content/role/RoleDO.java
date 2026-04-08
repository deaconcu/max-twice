package com.prosper.learn.content.role;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoleDO {

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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt; // 删除时间，NULL表示未删除（软删除字段）
}
