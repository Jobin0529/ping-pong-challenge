package com.example.pong.mq;

import com.example.common.model.PingPongMessage;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Pong 审计消息生产者
 * 
 * 功能：
 *   - 每次处理 Ping 请求后，发送审计消息到 RocketMQ
 *   - 记录请求的详细信息（时间、来源、消息ID等）
 */
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true")
public class PongAuditProducer {

    private static final Logger log = LoggerFactory.getLogger(PongAuditProducer.class);

    private final Producer producer;
    private final ClientServiceProvider provider;

    @Value("${rocketmq.producer.topic:pong-audit}")
    private String topic;

    @Value("${spring.application.name:pong-service}")
    private String serviceName;

    public PongAuditProducer(Producer producer) {
        this.producer = producer;
        this.provider = ClientServiceProvider.loadService();
        log.info("[PongAuditProducer] 初始化完成, topic={}", topic);
    }

    /**
     * 发送审计消息
     * 
     * @param pingMessage 收到的 Ping 消息
     * @param responseMessage 发送的 Pong 响应消息
     */
    public void sendAuditMessage(PingPongMessage pingMessage, PingPongMessage responseMessage) {
        String auditMessageId = java.util.UUID.randomUUID().toString();
        long timestamp = Instant.now().toEpochMilli();

        try {
            // 构建审计消息内容
            String auditContent = buildAuditContent(pingMessage, responseMessage, auditMessageId, timestamp);
            
            log.info("[PongAuditProducer] [Audit #{}] [{}] 准备发送审计消息...", auditMessageId, timestamp);
            log.info("[PongAuditProducer] [Audit #{}] 原始请求: messageId={}, source={}, payload={}", 
                    auditMessageId, pingMessage.getMessageId(), pingMessage.getSource(), pingMessage.getPayload());
            log.info("[PongAuditProducer] [Audit #{}] 响应消息: messageId={}, source={}, payload={}", 
                    auditMessageId, responseMessage.getMessageId(), responseMessage.getSource(), responseMessage.getPayload());

            // 构建 RocketMQ 消息
            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setBody(auditContent.getBytes(StandardCharsets.UTF_8))
                    .build();

            // 发送消息
            SendReceipt sendReceipt = producer.send(message);
            
            log.info("[PongAuditProducer] [Audit #{}] [{}] ✓ 审计消息发送成功, messageId={}", 
                    auditMessageId, timestamp, sendReceipt.getMessageId());

        } catch (Exception e) {
            log.error("[PongAuditProducer] [Audit #{}] [{}] ✗ 审计消息发送失败: {}", 
                    auditMessageId, timestamp, e.getMessage(), e);
        }
    }

    /**
     * 构建审计消息内容
     */
    private String buildAuditContent(PingPongMessage pingMessage, PingPongMessage responseMessage, 
                                     String auditMessageId, long timestamp) {
        return String.format(
                "{" +
                "\"auditId\":\"%s\"," +
                "\"timestamp\":%d," +
                "\"service\":\"%s\"," +
                "\"request\":{" +
                    "\"messageId\":\"%s\"," +
                    "\"source\":\"%s\"," +
                    "\"payload\":\"%s\"" +
                "}," +
                "\"response\":{" +
                    "\"messageId\":\"%s\"," +
                    "\"source\":\"%s\"," +
                    "\"payload\":\"%s\"" +
                "}" +
                "}",
                auditMessageId,
                timestamp,
                serviceName,
                pingMessage.getMessageId(),
                pingMessage.getSource(),
                pingMessage.getPayload(),
                responseMessage.getMessageId(),
                responseMessage.getSource(),
                responseMessage.getPayload()
        );
    }
}
