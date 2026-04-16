package com.prosper.learn.application.dto.request;

import lombok.Data;

/**
 * 前端错误上报请求
 */
@Data
public class FrontendErrorRequest {

    /** 错误类型 */
    private String errorType;

    /** 错误消息 */
    private String message;

    /** 堆栈信息 */
    private String stackTrace;

    /** 页面URL */
    private String url;

    /** 浏览器UA */
    private String userAgent;

    /** 额外数据（JSON） */
    private String extraData;
}
