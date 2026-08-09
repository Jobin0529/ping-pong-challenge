package com.example.ping.scheduler;

import com.example.common.model.RequestResult;
import com.example.ping.service.PingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ping 定时调度器 - 每隔 1 秒尝试向 Pong 服务发送 "Hello"
 *
 * 日志记录：
 *   - 每次请求尝试都会记录结果
 *   - 结果包括：成功响应 / 本地速率限制 / Pong 限流
 *   - 定期输出统计信息
 */
@Component
public class PingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PingScheduler.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final PingService pingService;

    // 统计计数器
    private final AtomicInteger totalAttempts = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger rateLimitedCount = new AtomicInteger(0);
    private final AtomicInteger throttledByPongCount = new AtomicInteger(0);
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

    public PingScheduler(PingService pingService) {
        this.pingService = pingService;
    }

    /**
     * 每隔 1 秒执行一次 Ping 请求
     */
    @Scheduled(fixedRate = 1000)
    public void scheduledPing() {
        String now = LocalDateTime.now().format(FORMATTER);
        int attemptNum = totalAttempts.incrementAndGet();

        log.info("========================================");
        log.info("[Scheduler] [{}] 第 {} 次请求尝试", now, attemptNum);

        RequestResult result = pingService.sendPing();

        // 根据结果更新统计
        switch (result) {
            case SENT_AND_RESPONDED:
                successCount.incrementAndGet();
                log.info("[Scheduler] [{}] 结果: 请求已发送 & Pong 响应 ✓", now);
                break;
            case RATE_LIMITED_LOCALLY:
                rateLimitedCount.incrementAndGet();
                log.warn("[Scheduler] [{}] 结果: 请求未发送 - 被本地跨进程速率限制 ✗", now);
                break;
            case THROTTLED_BY_PONG:
                throttledByPongCount.incrementAndGet();
                log.warn("[Scheduler] [{}] 结果: 请求已发送 & Pong 限流 (429) ✗", now);
                break;
        }

        // 每 10 次输出统计摘要
        if (attemptNum % 10 == 0) {
            printStatistics();
        }
    }

    /**
     * 打印统计摘要
     */
    private void printStatistics() {
        long uptime = (System.currentTimeMillis() - startTime.get()) / 1000;
        log.info("---------- 统计摘要 ----------");
        log.info("运行时间: {}秒", uptime);
        log.info("总尝试次数: {}", totalAttempts.get());
        log.info("成功响应: {}", successCount.get());
        log.info("本地限流: {}", rateLimitedCount.get());
        log.info("Pong限流: {}", throttledByPongCount.get());
        log.info("------------------------------");
    }

    // Getters for testing
    public int getTotalAttempts() { return totalAttempts.get(); }
    public int getSuccessCount() { return successCount.get(); }
    public int getRateLimitedCount() { return rateLimitedCount.get(); }
    public int getThrottledByPongCount() { return throttledByPongCount.get(); }
}
