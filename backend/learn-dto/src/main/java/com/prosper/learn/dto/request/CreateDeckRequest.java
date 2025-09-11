package com.prosper.learn.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建卡片组请求DTO
 */
@Data
public class CreateDeckRequest {

    @NotNull(message = "源帖子ID不能为空")
    private Long sourcePostId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255字符")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000字符")
    private String description;

    @NotEmpty(message = "卡片列表不能为空")
    @Valid
    private List<CardInfo> cards;

    @Data
    public static class CardInfo {
        @NotBlank(message = "卡片正面不能为空")
        @Size(max = 2000, message = "卡片正面长度不能超过2000字符")
        private String front;

        @NotBlank(message = "卡片背面不能为空")
        @Size(max = 2000, message = "卡片背面长度不能超过2000字符")
        private String back;
    }

}