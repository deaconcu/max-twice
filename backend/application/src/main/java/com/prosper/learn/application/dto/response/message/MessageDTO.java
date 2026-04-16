package com.prosper.learn.application.dto.response.message;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

@Data
public class MessageDTO {

    private Long id;

    private Long senderId;

    private UserBriefDTO sender;

    private long receiverId;

    private UserBriefDTO receiver;

    private String content;

    private Integer type;

    private String createdAt;

}
