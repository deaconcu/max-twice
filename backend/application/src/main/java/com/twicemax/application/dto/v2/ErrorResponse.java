package com.twicemax.application.dto.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * v2 统一错误响应体。
 * <p>
 * 形如：
 * <pre>
 * {
 *   "error": {
 *     "code": "INVITE_ONLY",
 *     "message": "TwiceMax 正在内测中...",
 *     "details": { ... }
 *   }
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private ErrorBody error;

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, null));
    }

    public static ErrorResponse of(String code, String message, Map<String, Object> details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;

        /**
         * 可选的附加信息（字段级错误、retryAfter 等）。
         * 为 null 时省略输出。
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> details;
    }
}
