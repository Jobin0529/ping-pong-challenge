package com.example.common.model;

import java.time.Instant;

/**
 * 消息模型 - Ping/Pong 服务间通信的标准化消息体
 */
public class PingPongMessage {

    private String type;          // "PING" or "PONG"
    private String payload;       // "Hello" or "World"
    private String source;        // 来源实例标识
    private long timestamp;       // 消息时间戳
    private String messageId;     // 消息唯一ID

    public PingPongMessage() {
    }

    public PingPongMessage(String type, String payload, String source) {
        this.type = type;
        this.payload = payload;
        this.source = source;
        this.timestamp = Instant.now().toEpochMilli();
        this.messageId = java.util.UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return String.format("PingPongMessage{type='%s', payload='%s', source='%s', messageId='%s'}",
                type, payload, source, messageId);
    }
}
