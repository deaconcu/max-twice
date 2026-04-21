package com.twicemax.analytics.monitoring;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错误日志数据对象
 */
@Data
public class ErrorLogDO {

    /** 主键ID */
    private Long id;

    /** 错误指纹，用于去重 */
    private String fingerprint;

    /** 来源：frontend / backend */
    private String source;

    /** 异常类名 / Error类型 */
    private String errorType;

    /** 错误消息 */
    private String message;

    /** 完整堆栈 */
    private String stackTrace;

    /** 请求URL / 页面URL */
    private String url;

    /** 用户ID（可空） */
    private Long userId;

    /** IP地址 */
    private String ip;

    /** 浏览器UA（前端用） */
    private String userAgent;

    /** 额外信息（JSON） */
    private String extraData;

    /** 发生次数 */
    private Integer count;

    /** 首次发生时间 */
    private LocalDateTime firstSeenAt;

    /** 最近发生时间 */
    private LocalDateTime lastSeenAt;

    /** 状态：new/ignored/resolved */
    private String status;
}
