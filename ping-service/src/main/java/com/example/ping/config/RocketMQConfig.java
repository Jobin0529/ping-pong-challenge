package com.example.ping.config;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * RocketMQ 配置类 - Ping 服务（消费者）
 * 
 * 功能：
 *   - 配置 RocketMQ 客户端
 *   - 创建 SimpleConsumer Bean 用于接收审计消息
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true")
public class RocketMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConfig.class);

    @Value("${rocketmq.endpoints:localhost:8081}")
    private String endpoints;

    @Value("${rocketmq.consumer.group:ping-consumer-group}")
    private String consumerGroup;

    @Value("${rocketmq.consumer.topic:pong-audit}")
    private String consumerTopic;

    /**
     * 创建 RocketMQ SimpleConsumer
     */
    @Bean
    public org.apache.rocketmq.client.apis.consumer.SimpleConsumer rocketMQConsumer() throws Exception {
        log.info("[RocketMQ] 初始化 SimpleConsumer, endpoints={}, group={}, topic={}", 
                endpoints, consumerGroup, consumerTopic);
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(endpoints)
                .build();

        FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);

        SimpleConsumerBuilder builder = provider.newSimpleConsumerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setConsumerGroup(consumerGroup)
                .setAwaitDuration(java.time.Duration.ofSeconds(15))
                .setSubscriptionExpressions(Collections.singletonMap(consumerTopic, filterExpression));

        org.apache.rocketmq.client.apis.consumer.SimpleConsumer consumer = builder.build();
        log.info("[RocketMQ] ✓ SimpleConsumer 初始化成功");
        
        return consumer;
    }
}
