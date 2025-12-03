package com.prosper.learn.api.ratelimit;

/**
 * 限流类型枚举
 *
 * @author Claude Code
 */
public enum LimitType {
    /**
     * 按用户ID限流（需要登录）
     */
    USER,

    /**
     * 按IP地址限流
     */
    IP,

    /**
     * 全局限流（所有请求共享）
     */
    GLOBAL
}
