package com.prosper.learn.application.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemConfigDTO {

    private String key;

    private String value;

    private LocalDateTime updatedAt;
}
