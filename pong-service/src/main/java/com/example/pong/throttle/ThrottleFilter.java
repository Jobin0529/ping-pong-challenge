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
 * 节流过滤器（Throttle Filter）- 限制 Pong 服务每秒只能处理 1 个请求
 *
 * <p>实现原理（固定窗口限流算法）：
 *   - 使用 AtomicInteger 记录当前秒内已通过的请求数
 *   - 使用 AtomicLong 记录当前正在计数的秒时间戳（从 Unix 纪元开始的秒数）
 *   - 每次请求到达时，判断是否进入了新的一秒，若是则通过 CAS 重置计数器
 *   - 若同一秒内请求数超过限制值（1），直接返回 429 Too Many Requests
 *
 * <p>线程安全保证：
 *   - 所有共享状态（currentSecond、requestCount）均使用原子类，无锁并发安全
 *   - 使用 CAS（compareAndSet）防止多线程同时重置计数器导致的计数错误
 *
 * <p>注意：此实现仅在单 JVM 进程内生效，无法在分布式多实例间共享限流状态
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)// 指定过滤器的执行优先级为最高（最先执行）
public class ThrottleFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(ThrottleFilter.class);

    /** 节流限制值：每秒最多 1 个请求 */
    private static final int THROTTLE_LIMIT = 1;

    /** 当前秒的时间戳 */
    private final AtomicLong currentSecond = new AtomicLong(Instant.now().getEpochSecond());

    /**
     * 当前秒内已通过放行的请求计数
     * <p>使用 AtomicInteger 保证多线程下的原子递增和读取
     * 每当进入新的一秒，该值会被重置为 0
     */
    private final AtomicInteger requestCount = new AtomicInteger(0);

    /**
     * WebFilter 的核心过滤方法
     *
     * <p>执行流程：
     *   1. 过滤路径：只对 /api/pong 开头的请求进行限流，其余直接放行
     *   2. 获取当前秒，与记录的秒比较
     *   3. 若进入新秒，通过 CAS 尝试重置计数器（仅第一个到达的线程成功重置）
     *   4. 原子递增请求计数
     *   5. 若计数超过阈值，返回 429 状态码并结束请求
     *   6. 若未超限，将请求传递给后续过滤器链继续处理
     *
     * @param exchange 当前的 HTTP 请求-响应交换对象，包含请求和响应的所有信息
     * @param chain    过滤器链，用于将请求传递给下一个过滤器或最终的处理器
     * @return Mono<Void> 表示响应完成的响应式流信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 获取请求路径，例如 "/api/pong/v1/status"
        String path = exchange.getRequest().getPath().value();

        // ----- 步骤1：路径过滤 -----
        // 只对 /api/pong 路径下的请求执行限流，其他请求（如健康检查、静态资源）直接放行
        // 这样可以避免误伤非业务接口，也减少了不必要的原子操作开销
        if (!path.startsWith("/api/pong")) {
            return chain.filter(exchange);
        }

        // ----- 步骤2：获取当前时间秒数 -----
        long nowSecond = Instant.now().getEpochSecond();
        long prevSecond = currentSecond.get();

        // ----- 步骤3：检测是否进入新的一秒（核心 CAS 重置逻辑） -----
        if (nowSecond != prevSecond) {
            // 新的一秒，尝试重置计数器
            if (currentSecond.compareAndSet(prevSecond, nowSecond)) {
                requestCount.set(0);
                log.debug("[Throttle] 新的一秒开始, second={}, 计数器已重置", nowSecond);
            }
        }

        // 原子递增并检查是否超限
        int count = requestCount.incrementAndGet();

        // ----- 步骤5：判断是否被限流 -----
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
