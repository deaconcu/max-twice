package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.UserDTOV4;
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

    private Integer isRead;

    private String createdAt;

}
