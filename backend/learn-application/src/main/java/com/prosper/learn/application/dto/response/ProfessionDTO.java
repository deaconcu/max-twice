package com.prosper.learn.application.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfessionDTO {

    private Long id;

    private String name;

    private String description;

    private String price;

    private String skills;

    private Integer mainCategory;

    private Integer subCategory;

    private Byte state; // SUBMITTED=0, APPROVED=1, REJECTED=2

    private String reason; // 拒绝原因，默认为空字符串

    private String icon; // 图标字段

    private Long creatorId; // 创建者 ID

    private Integer learnerCount; // 学习人数

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
