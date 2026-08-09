package com.example.pong.config;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.ProducerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类 - Pong 服务（生产者）
 * 
 * 功能：
 *   - 配置 RocketMQ 客户端
 *   - 创建 Producer Bean 用于发送审计消息
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true")
public class RocketMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConfig.class);

    @Value("${rocketmq.endpoints:localhost:8081}")
    private String endpoints;

    @Value("${rocketmq.producer.topic:pong-audit}")
    private String producerTopic;

    /**
     * 创建 RocketMQ Producer
     */
    @Bean
    public org.apache.rocketmq.client.apis.producer.Producer rocketMQProducer() throws Exception {
        log.info("[RocketMQ] 初始化 Producer, endpoints={}, topic={}", endpoints, producerTopic);
        
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(endpoints)
                .build();

        ProducerBuilder builder = provider.newProducerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setTopics(producerTopic);

        org.apache.rocketmq.client.apis.producer.Producer producer = builder.build();
        log.info("[RocketMQ] ✓ Producer 初始化成功");
        
        return producer;
    }
}
