package com.prosper.learn.shared.dto.request;

import com.prosper.learn.common.validation.ConfigurableSize;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @ConfigurableSize(configKey = "username")
    private String name;

    @ConfigurableSize(configKey = "biography")
    private String biography;
}