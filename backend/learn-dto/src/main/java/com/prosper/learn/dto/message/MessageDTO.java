package com.prosper.learn.dto.message;

import com.prosper.learn.dto.UserDTOV1;
import com.prosper.learn.dto.UserDTOV4;
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
