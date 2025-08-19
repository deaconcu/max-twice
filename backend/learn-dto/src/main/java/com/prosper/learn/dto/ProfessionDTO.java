package com.prosper.learn.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfessionDTO {

    private Integer id;

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

    private Integer learnerCount; // 学习人数

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
