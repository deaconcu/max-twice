package com.prosper.learn.application.dto.response.profession;

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

    private String icon; // 图标字段

    private Integer learnerCount; // 学习人数

    private Boolean bookmarked; // 是否已收藏

    private LocalDateTime createdAt;
}
