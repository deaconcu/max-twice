package com.prosper.learn.persistence.dataobject;

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

    private String state; // SUBMITED, APPROVED, REJECTED

    private String rejectedReason; // 拒绝原因，默认为空字符串

    private String icon; // 图标字段

    private Long creator; // 创建者 ID

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
