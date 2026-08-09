package com.example.common.model;

/**
 * 请求结果枚举 - 用于日志记录
 */
public enum RequestResult {

    /**
     * 请求已发送 & Pong 正常响应
     */
    SENT_AND_RESPONDED("SENT_AND_RESPONDED", "请求已发送，Pong 正常响应"),

    /**
     * 请求未发送 - 被本地跨进程速率限制
     */
    RATE_LIMITED_LOCALLY("RATE_LIMITED_LOCALLY", "请求未发送 - 被本地跨进程速率限制"),

    /**
     * 请求已发送 & Pong 返回 429 限流
     */
    THROTTLED_BY_PONG("THROTTLED_BY_PONG", "请求已发送，Pong 返回 429 限流");

    private final String code;
    private final String description;

    RequestResult(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
