package com.twicemax.application.dto.response;

import lombok.Data;

/**
 * 错误日志 DTO
 */
@Data
public class ErrorLogDTO {

    private Long id;
    private String fingerprint;
    private String source;
    private String errorType;
    private String message;
    private String stackTrace;
    private String url;
    private Long userId;
    private String ip;
    private String userAgent;
    private String extraData;
    private Integer count;
    private String firstSeenAt;
    private String lastSeenAt;
    private String status;
}
