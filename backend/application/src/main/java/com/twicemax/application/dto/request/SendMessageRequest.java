package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class SendMessageRequest {

    @NotNull(message = "消息类型不能为空")
    private Integer type;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "消息内容不能为空")
    @ConfigurableSize(configKey = "message-content")
    private String content;
}