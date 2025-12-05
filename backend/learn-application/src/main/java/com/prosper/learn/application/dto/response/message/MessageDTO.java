package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.UserDTO;
import lombok.Data;

@Data
public class MessageDTO {

    private Long id;

    private Long senderId;

    private UserDTO sender;

    private long receiverId;

    private UserDTO receiver;

    private String content;

    private Integer type;

    private String createdAt;

}
