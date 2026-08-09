package com.example.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ping 服务启动类
 *
 * 功能：每隔 1 秒向 Pong 服务发送 "Hello"
 * 跨进程速率限制：所有 Ping 进程合计最多 2 RPS（使用 FileLock）
 */
@SpringBootApplication(scanBasePackages = {"com.example.ping", "com.example.common"})
@EnableScheduling
public class PingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PingServiceApplication.class, args);
    }
}
