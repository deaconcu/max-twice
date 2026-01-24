package com.prosper.learn.interaction.bookmark;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookmarkDO {

    private Long id;

    private Long userId;

    private Long objectId;

    private Integer objectType;

    private Long parentId;

    private LocalDateTime createdAt;
}
