package com.twicemax.application.dto.response.message;

import lombok.Data;

import java.util.List;

/**
 * 消息列表响应 DTO
 * 包含消息列表和最后查看的消息ID
 */
@Data
public class MessageListResponse {

    /**
     * 消息列表
     */
    private List<MessageDTO> messages;

    /**
     * 最后查看的消息ID（只在第一页返回，用于前端标记NEW）
     */
    private Long lastViewedMessageId;

    public static MessageListResponse of(List<MessageDTO> messages, Long lastViewedMessageId) {
        MessageListResponse response = new MessageListResponse();
        response.setMessages(messages);
        response.setLastViewedMessageId(lastViewedMessageId);
        return response;
    }
}
