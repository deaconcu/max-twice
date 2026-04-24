package com.twicemax.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新当前用户偏好语言请求。
 * <p>
 * 合法取值限定在前端/后端共同约定的白名单内（zh / en），更复杂的语言协商（如 zh-CN vs zh-TW）
 * 在产品真正支持多区域时再扩展。
 */
@Data
public class UpdateLocaleRequest {

    @NotBlank(message = "语言不能为空")
    @Pattern(regexp = "^(zh|en)$", message = "不支持的语言")
    private String locale;
}
