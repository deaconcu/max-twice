package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContentsDO {

    private Long id;

    private Long userId;

    private String contents;

    private LocalDateTime cTime;

    private LocalDateTime uTime;
}
