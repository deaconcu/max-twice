package com.prosper.learn.dto.message;

import com.prosper.learn.dto.UserDTOV1;
import com.prosper.learn.dto.UserDTOV4;
import lombok.Data;

@Data
public class MessageDTO {

    private int id;

    private UserDTOV4 sender;

    private UserDTOV4 receiver;

    private int type;

    private int isRead;

    private String createdAt;

}
