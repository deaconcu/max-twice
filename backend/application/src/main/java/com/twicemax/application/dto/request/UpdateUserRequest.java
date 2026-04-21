package com.twicemax.application.dto.request;

import com.twicemax.shared.common.validator.ConfigurableSize;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @ConfigurableSize(configKey = "username")
    private String name;

    @ConfigurableSize(configKey = "biography")
    private String biography;

    @Size(max = 50, message = "时区长度不能超过50")
    private String timezone;
}