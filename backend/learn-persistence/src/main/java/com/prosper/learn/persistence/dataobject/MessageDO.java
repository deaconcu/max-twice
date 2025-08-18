package com.prosper.learn.persistence.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDO {

    private int id;

    private int senderId;

    private int receiverId;

    private String content;

    private int type;

    private int isRead;

    private LocalDateTime createdAt;

}
