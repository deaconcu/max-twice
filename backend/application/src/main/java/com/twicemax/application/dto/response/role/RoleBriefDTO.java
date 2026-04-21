package com.twicemax.application.dto.response.role;

import lombok.Data;

/**
 * 专业简要信息 DTO
 * 用于路线图、课程等列表展示
 */
@Data
public class RoleBriefDTO {

    private Long id;

    private String name;

    private String icon;
}
