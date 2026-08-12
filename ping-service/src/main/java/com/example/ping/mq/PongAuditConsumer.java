package com.example.ping.mq;

import javax.annotation.PostConstruct;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Pong 审计消息消费者
 * 
 * 功能：
 *   - 定期从 RocketMQ 拉取审计消息
 *   - 处理并记录审计信息
 */
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true")
public class PongAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(PongAuditConsumer.class);

    private final SimpleConsumer consumer;

    @Value("${rocketmq.consumer.topic:pong-audit}")
    private String topic;

    public PongAuditConsumer(SimpleConsumer consumer) {
        this.consumer = consumer;
    }

    @PostConstruct
    public void init() {
        log.info("[PongAuditConsumer] 初始化完成, topic={}", topic);
    }

    /**
     * 每 5 秒拉取一次审计消息
     */
    @Scheduled(fixedDelay = 5000)
    public void consumeAuditMessages() {
        try {
            log.debug("[PongAuditConsumer] 开始拉取审计消息...");
            
            // 拉取消息（最多 10 条，等待 5 秒）
            List<MessageView> messages = consumer.receive(10, Duration.ofSeconds(5));
            
            if (messages.isEmpty()) {
                log.debug("[PongAuditConsumer] 没有新的审计消息");
                return;
            }

            log.info("[PongAuditConsumer] 收到 {} 条审计消息", messages.size());

            for (MessageView message : messages) {
                processMessage(message);
            }

        } catch (Exception e) {
            log.error("[PongAuditConsumer] 拉取审计消息时发生异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理单条审计消息
     */
    private void processMessage(MessageView message) {
        String messageId = message.getMessageId().toString();
        
        try {
            // 读取消息内容
            byte[] body = message.getBody().array();
            String content = new String(body, StandardCharsets.UTF_8);
            
            log.info("[PongAuditConsumer] ========== 审计消息 ==========");
            log.info("[PongAuditConsumer] MessageId: {}", messageId);
            log.info("[PongAuditConsumer] Topic: {}", message.getTopic());
            log.info("[PongAuditConsumer] Tag: {}", message.getTag().orElse("N/A"));
            log.info("[PongAuditConsumer] 内容: {}", content);
            log.info("[PongAuditConsumer] ==============================");

            // 确认消息已消费
            consumer.ack(message);
            log.info("[PongAuditConsumer] ✓ 消息 {} 已确认", messageId);

        } catch (Exception e) {
            log.error("[PongAuditConsumer] ✗ 处理消息 {} 失败: {}", messageId, e.getMessage(), e);
        }
    }
}
