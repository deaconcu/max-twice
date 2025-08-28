package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDO {

    private Long id;

    private Long senderId;

    private Long receiverId;

    private String content;

    private Integer type;

    private Integer isRead;

    private LocalDateTime createdAt;

}
