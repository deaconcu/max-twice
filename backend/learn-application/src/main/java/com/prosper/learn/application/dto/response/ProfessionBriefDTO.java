package com.prosper.learn.application.dto.response;

import lombok.Data;

/**
 * 专业简要信息 DTO
 * 用于路线图、课程等列表展示
 */
@Data
public class ProfessionBriefDTO {

    private Long id;

    private String name;

    private String icon;
}
