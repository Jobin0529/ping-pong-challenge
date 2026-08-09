package com.example.ping.config;

import com.example.common.ratelimit.FileLockRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨进程速率限制器配置
 *
 * 使用 Java FileLock 实现跨 JVM 进程的速率控制
 * 所有 Ping 进程共享同一个锁文件，合计限制为 2 RPS
 */
@Configuration
public class RateLimiterConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterConfig.class);

    @Value("${rate.limiter.lock.file:/tmp/ping-pong-rate.lock}")
    private String lockFile;

    @Value("${rate.limiter.max.per.second:2}")
    private int maxPerSecond;

    @Bean
    public FileLockRateLimiter fileLockRateLimiter() {
        log.info("[RateLimiter] 初始化跨进程速率限制器, lockFile={}, maxPerSecond={}", lockFile, maxPerSecond);
        return new FileLockRateLimiter(lockFile, maxPerSecond);
    }
}
