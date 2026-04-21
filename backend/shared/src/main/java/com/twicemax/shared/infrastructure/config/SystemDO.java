package com.twicemax.shared.infrastructure.config;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemDO {

    private String key;

    private String value;

    private LocalDateTime updatedAt;

}
