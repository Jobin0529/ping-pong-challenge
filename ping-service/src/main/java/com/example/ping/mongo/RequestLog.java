package com.example.ping.mongo;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "request_logs")
public class RequestLog {

    @Id
    private String id;

    // 请求信息
    private String messageId;
    private String source;
    private String payload;

    // 响应信息
    private String responseMessageId;
    private String responseSource;
    private String responsePayload;

    // 请求结果
    private String result; // SUCCESS, RATE_LIMITED, TIMEOUT, ERROR
    private String errorMessage;

    // 性能指标
    private Long responseTimeMs;

    // 实例信息
    private String serviceName;
    private String instanceId;
    private Integer serverPort;

    // 时间戳
    private Long timestamp;
    private Instant createdAt;

    // 限流信息
    private Boolean rateLimited;
    private String rateLimitReason;
}
