package com.example.pong.controller;

import com.example.common.model.PingPongMessage;
import com.example.pong.mq.PongAuditProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Pong 控制器 - 处理 Ping 请求
 * 
 * 功能：
 *   - 接收 Ping 请求
 *   - 返回 Pong 响应
 *   - 如果启用 RocketMQ，发送审计消息
 */
@RestController
@RequestMapping("/api")
public class PongController {

    private static final Logger log = LoggerFactory.getLogger(PongController.class);

    @Value("${spring.application.name:pong-service}")
    private String serviceName;

    @Autowired(required = false)
    private PongAuditProducer auditProducer;

    @PostMapping("/pong")
    public Mono<PingPongMessage> handlePing(@RequestBody PingPongMessage pingMessage) {
        // 如果请求中带有 messageId（例如来自上游的追踪ID），则沿用；
        // 否则生成新的 UUID，用于全链路日志关联
        String messageId = pingMessage.getMessageId() != null ? 
                pingMessage.getMessageId() : java.util.UUID.randomUUID().toString();
        long timestamp = Instant.now().toEpochMilli();

        log.info("[PongController] [{}] [{}] 收到 Ping 请求: source={}, payload={}", 
                messageId, timestamp, pingMessage.getSource(), pingMessage.getPayload());

        // 构建 Pong 响应
        PingPongMessage pongResponse = new PingPongMessage(
                "PONG",
                "World",
                serviceName
        );
        pongResponse.setMessageId(messageId);
        pongResponse.setTimestamp(timestamp);

        log.info("[PongController] [{}] [{}] 发送 Pong 响应: source={}, payload={}", 
                messageId, timestamp, pongResponse.getSource(), pongResponse.getPayload());

        // 如果启用了 RocketMQ，发送审计消息
        if (auditProducer != null) {
            log.info("[PongController] [{}] [{}] RocketMQ 已启用，准备发送审计消息", messageId, timestamp);
            try {
                auditProducer.sendAuditMessage(pingMessage, pongResponse);
            } catch (Exception e) {
                log.error("[PongController] [{}] [{}] 发送审计消息时发生异常: {}", 
                        messageId, timestamp, e.getMessage(), e);
            }
        } else {
            log.debug("[PongController] [{}] [{}] RocketMQ 未启用，跳过审计消息", messageId, timestamp);
        }

        return Mono.just(pongResponse);
    }
}
