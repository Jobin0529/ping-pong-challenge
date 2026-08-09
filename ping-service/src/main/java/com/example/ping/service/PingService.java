package com.example.ping.service;

import com.example.common.model.PingPongMessage;
import com.example.common.model.RequestResult;
import com.example.common.ratelimit.FileLockRateLimiter;
import com.example.ping.mongo.MongoLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Ping 服务 - 负责向 Pong 服务发送 "Hello" 请求
 *
 * 核心逻辑：
 *   1. 先通过 FileLockRateLimiter 检查跨进程速率限制
 *   2. 如果通过限制，发送 HTTP POST 到 Pong 服务
 *   3. 根据响应结果记录日志（成功/被Pong限流）
 *   4. 异步保存日志到 MongoDB
 */
@Service
public class PingService {

    private static final Logger log = LoggerFactory.getLogger(PingService.class);

    private final WebClient pongWebClient;
    private final FileLockRateLimiter rateLimiter;
    private final MongoLogService mongoLogService;

    @Value("${spring.application.name:ping-service}")
    private String serviceName;

    @Value("${pong.service.endpoint:/api/pong}")
    private String pongEndpoint;

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Autowired
    public PingService(WebClient pongWebClient, FileLockRateLimiter rateLimiter, 
                       MongoLogService mongoLogService) {
        this.pongWebClient = pongWebClient;
        this.rateLimiter = rateLimiter;
        this.mongoLogService = mongoLogService;
        log.info("[PingService] 初始化完成, MongoDB 日志服务已集成");
    }

    /**
     * 发送一次 Ping 请求到 Pong 服务
     *
     * @return 请求结果枚举
     */
    public RequestResult sendPing() {
        String messageId = java.util.UUID.randomUUID().toString();
        long timestamp = Instant.now().toEpochMilli();
        long startTime = System.currentTimeMillis();
        String instanceId = serviceName + "-" + serverPort;

        // Step 1: 检查跨进程速率限制
        log.info("[Ping #{}] [{}] 尝试获取跨进程速率限制许可...", messageId, timestamp);
        boolean acquired = rateLimiter.tryAcquire();

        if (!acquired) {
            log.warn("[Ping #{}] [{}] 请求未发送 - 被本地跨进程速率限制 (RATE_LIMITED_LOCALLY)",
                    messageId, timestamp);
            
            // 保存限流日志到 MongoDB
            PingPongMessage rateLimitedMsg = new PingPongMessage("PING", "Hello", serviceName);
            rateLimitedMsg.setMessageId(messageId);
            rateLimitedMsg.setTimestamp(timestamp);
            mongoLogService.logRateLimitedRequest(rateLimitedMsg, "跨进程速率限制", instanceId);
            
            return RequestResult.RATE_LIMITED_LOCALLY;
        }

        log.info("[Ping #{}] [{}] 速率限制许可获取成功，准备发送请求...", messageId, timestamp);

        // Step 2: 构建 Ping 消息
        PingPongMessage pingMessage = new PingPongMessage("PING", "Hello", serviceName);
        pingMessage.setMessageId(messageId);
        pingMessage.setTimestamp(timestamp);

        // Step 3: 发送 HTTP 请求到 Pong 服务
        try {
            PingPongMessage response = pongWebClient.post()
                    .uri(pongEndpoint)
                    .bodyValue(pingMessage)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        if (clientResponse.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            log.warn("[Ping #{}] [{}] 请求已发送，Pong 返回 429 限流 (THROTTLED_BY_PONG)",
                                    messageId, timestamp);
                            return Mono.error(new ThrottledException("Pong returned 429"));
                        }
                        return Mono.error(new RuntimeException(
                                "Pong returned error: " + clientResponse.statusCode()));
                    })
                    .bodyToMono(PingPongMessage.class)
                    .block();

            long responseTimeMs = System.currentTimeMillis() - startTime;

            if (response != null) {
                log.info("[Ping #{}] [{}] 请求已发送 & Pong 响应: payload={}, source={} (SENT_AND_RESPONDED)",
                        messageId, timestamp, response.getPayload(), response.getSource());
                
                // 保存成功日志到 MongoDB
                mongoLogService.logSuccessRequest(pingMessage, response, responseTimeMs, instanceId);
            } else {
                log.warn("[Ping #{}] [{}] 请求已发送，但 Pong 响应为空", messageId, timestamp);
                
                // 保存错误日志到 MongoDB
                mongoLogService.logErrorRequest(pingMessage, "Pong 响应为空", responseTimeMs, instanceId);
            }

            return RequestResult.SENT_AND_RESPONDED;

        } catch (ThrottledException e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            
            // 保存被 Pong 限流的日志到 MongoDB
            mongoLogService.logThrottledRequest(pingMessage, responseTimeMs, instanceId);
            
            return RequestResult.THROTTLED_BY_PONG;
        } catch (Exception e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;
            log.error("[Ping #{}] [{}] 请求发送异常: {}", messageId, timestamp, e.getMessage(), e);
            
            // 保存错误日志到 MongoDB
            mongoLogService.logErrorRequest(pingMessage, e.getMessage(), responseTimeMs, instanceId);
            
            return RequestResult.THROTTLED_BY_PONG;
        }
    }

    /**
     * 自定义异常 - Pong 服务限流
     */
    public static class ThrottledException extends RuntimeException {
        public ThrottledException(String message) {
            super(message);
        }
    }
}
