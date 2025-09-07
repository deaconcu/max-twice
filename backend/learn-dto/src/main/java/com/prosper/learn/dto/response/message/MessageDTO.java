package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.UserDTO;
import com.prosper.learn.dto.response.old.UserDTOV4;
import lombok.Data;

@Data
public class MessageDTO {

    private Long id;

    private UserDTO sender;

    private UserDTO receiver;

    private Integer type;

    private Integer isRead;

    private String createdAt;

}
