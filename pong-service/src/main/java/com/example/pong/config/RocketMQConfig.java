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
 * 注意：此配置仅在 application.yml中设置了 rocketmq.enabled=true 时才会生效，
 *  *       便于在不同环境（如本地测试）灵活开关。
 */
@Configuration
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true")
public class RocketMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RocketMQConfig.class);

    // 从 Spring 环境变量中注入 RocketMQ 服务端地址（endpoints）
    // 默认值为 "localhost:8081"，可在配置文件中通过 rocketmq.endpoints 覆盖
    @Value("${rocketmq.endpoints:localhost:8081}")
    private String endpoints;

    // 注入生产者发送消息的目标主题（Topic）
    // 默认值为 "pong-audit"，可通过 rocketmq.producer.topic 覆盖
    @Value("${rocketmq.producer.topic:pong-audit}")
    private String producerTopic;

    /**
     * 创建 RocketMQ 生产者（Producer）Bean
     *
     * 该 Bean 会被注入到其他服务类中（如 AuditService），用于发送消息。
     *
     * @return 配置好的 Producer 实例
     * @throws Exception 客户端初始化失败时抛出（如网络不通、认证失败等）
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
