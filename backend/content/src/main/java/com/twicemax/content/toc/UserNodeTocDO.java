package com.twicemax.content.toc;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserNodeTocDO {

    private Long id;

    private Long userId;

    private Long nodeId;

    private String toc;

    private LocalDateTime updatedAt;
}
