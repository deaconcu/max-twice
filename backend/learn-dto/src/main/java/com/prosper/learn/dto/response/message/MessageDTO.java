package com.prosper.learn.dto.response.message;

import com.prosper.learn.dto.response.UserDTOV4;
import lombok.Data;

@Data
public class MessageDTO {

    private Long id;

    private UserDTOV4 sender;

    private UserDTOV4 receiver;

    private Integer type;

    private Integer isRead;

    private String createdAt;

}
