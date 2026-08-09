package com.example.pong.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 节流过滤器 - 限制 Pong 服务每秒只能处理 1 个请求
 *
 * 实现原理：
 *   - 使用 AtomicInteger 记录当前秒的请求数
 *   - 使用 AtomicLong 记录当前秒的时间戳
 *   - 当新的一秒开始时，重置计数器
 *   - 当同一秒内请求数超过限制值时，返回 429 Too Many Requests
 *
 * 注意：此实现为单 JVM 内的节流控制
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ThrottleFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ThrottleFilter.class);

    /** 节流限制值：每秒最多 1 个请求 */
    private static final int THROTTLE_LIMIT = 1;

    /** 当前秒的时间戳 */
    private final AtomicLong currentSecond = new AtomicLong(Instant.now().getEpochSecond());

    /** 当前秒已处理的请求数 */
    private final AtomicInteger requestCount = new AtomicInteger(0);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 仅对 /api/pong 端点应用节流控制
        if (!path.startsWith("/api/pong")) {
            return chain.filter(exchange);
        }

        long nowSecond = Instant.now().getEpochSecond();
        long prevSecond = currentSecond.get();

        if (nowSecond != prevSecond) {
            // 新的一秒，尝试重置计数器
            if (currentSecond.compareAndSet(prevSecond, nowSecond)) {
                requestCount.set(0);
                log.debug("[Throttle] 新的一秒开始, second={}, 计数器已重置", nowSecond);
            }
        }

        // 原子递增并检查是否超限
        int count = requestCount.incrementAndGet();

        if (count > THROTTLE_LIMIT) {
            log.warn("[Throttle] 请求被限流! second={}, count={}/{}, path={}",
                    nowSecond, count, THROTTLE_LIMIT, path);

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        log.info("[Throttle] 请求通过, second={}, count={}/{}, path={}",
                nowSecond, count, THROTTLE_LIMIT, path);

        return chain.filter(exchange);
    }

    /**
     * 获取当前秒的请求计数（用于测试和监控）
     */
    public int getCurrentCount() {
        return requestCount.get();
    }

    /**
     * 获取当前记录的秒数
     */
    public long getCurrentSecond() {
        return currentSecond.get();
    }

    /**
     * 获取节流限制值
     */
    public int getThrottleLimit() {
        return THROTTLE_LIMIT;
    }
}
