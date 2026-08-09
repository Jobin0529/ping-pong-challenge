package com.example.pong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pong 服务启动类
 *
 * 功能：接收 Ping 服务的 "Hello" 请求，以 "World" 响应
 * 节流控制：限制为 1 RPS，超出返回 429
 */
@SpringBootApplication(scanBasePackages = {"com.example.pong", "com.example.common"})
public class PongServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PongServiceApplication.class, args);
    }
}
