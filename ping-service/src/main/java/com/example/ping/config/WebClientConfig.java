package com.example.ping.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 配置 - 用于调用 Pong 服务
 */
@Configuration
public class WebClientConfig {

    @Value("${pong.service.url:http://localhost:8081}")
    private String pongServiceUrl;

    @Bean
    public WebClient pongWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl(pongServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
