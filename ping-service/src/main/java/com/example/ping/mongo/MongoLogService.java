package com.example.ping.mongo;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.common.model.PingPongMessage;
import com.example.common.model.RequestResult;

@Service
public class MongoLogService {

    private static final Logger log = LoggerFactory.getLogger(MongoLogService.class);

    private final RequestLogRepository repository;

    @Value("${spring.application.name:ping-service}")
    private String serviceName;

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Autowired
    public MongoLogService(RequestLogRepository repository) {
        this.repository = repository;
        log.info("[MongoLogService] 初始化完成");
    }

    /**
     * 保存成功请求的日志
     */
    public void logSuccessRequest(PingPongMessage request, PingPongMessage response, 
                                  long responseTimeMs, String instanceId) {
        try {
            RequestLog logEntry = buildLogEntry(request, response, RequestResult.SENT_AND_RESPONDED, 
                                               null, responseTimeMs, instanceId, false, null);
            repository.save(logEntry);
            log.debug("[MongoLogService] ✓ 成功请求日志已保存, messageId={}", request.getMessageId());
        } catch (Exception e) {
            log.error("[MongoLogService] ✗ 保存成功请求日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存被限流请求的日志
     */
    public void logRateLimitedRequest(PingPongMessage request, String reason, 
                                     String instanceId) {
        try {
            RequestLog logEntry = buildLogEntry(request, null, RequestResult.RATE_LIMITED_LOCALLY, 
                                               null, 0L, instanceId, true, reason);
            repository.save(logEntry);
            log.debug("[MongoLogService] ✓ 限流请求日志已保存, messageId={}", request.getMessageId());
        } catch (Exception e) {
            log.error("[MongoLogService] ✗ 保存限流请求日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存被 Pong 限流请求的日志
     */
    public void logThrottledRequest(PingPongMessage request, long responseTimeMs, 
                                   String instanceId) {
        try {
            RequestLog logEntry = buildLogEntry(request, null, RequestResult.THROTTLED_BY_PONG, 
                                               "被 Pong 服务限流 (429)", responseTimeMs, instanceId, false, null);
            repository.save(logEntry);
            log.debug("[MongoLogService] ✓ 被 Pong 限流请求日志已保存, messageId={}", request.getMessageId());
        } catch (Exception e) {
            log.error("[MongoLogService] ✗ 保存被 Pong 限流请求日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存错误请求的日志
     */
    public void logErrorRequest(PingPongMessage request, String errorMessage, 
                               long responseTimeMs, String instanceId) {
        try {
            // 使用 THROTTLED_BY_PONG 作为错误情况的默认结果
            RequestLog logEntry = buildLogEntry(request, null, RequestResult.THROTTLED_BY_PONG, 
                                               errorMessage, responseTimeMs, instanceId, false, null);
            repository.save(logEntry);
            log.debug("[MongoLogService] ✓ 错误请求日志已保存, messageId={}", request.getMessageId());
        } catch (Exception e) {
            log.error("[MongoLogService] ✗ 保存错误请求日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建日志条目
     */
    private RequestLog buildLogEntry(PingPongMessage request, PingPongMessage response,
                                    RequestResult result, String errorMessage,
                                    long responseTimeMs, String instanceId,
                                    boolean rateLimited, String rateLimitReason) {
        return RequestLog.builder()
                .messageId(request.getMessageId())
                .source(request.getSource())
                .payload(request.getPayload())
                .responseMessageId(response != null ? response.getMessageId() : null)
                .responseSource(response != null ? response.getSource() : null)
                .responsePayload(response != null ? response.getPayload() : null)
                .result(result.name())
                .errorMessage(errorMessage)
                .responseTimeMs(responseTimeMs)
                .serviceName(serviceName)
                .instanceId(instanceId)
                .serverPort(serverPort)
                .timestamp(request.getTimestamp())
                .createdAt(Instant.now())
                .rateLimited(rateLimited)
                .rateLimitReason(rateLimitReason)
                .build();
    }

    /**
     * 根据 messageId 查询日志
     */
    public List<RequestLog> findByMessageId(String messageId) {
        return repository.findByMessageId(messageId);
    }

    /**
     * 根据结果类型查询日志
     */
    public List<RequestLog> findByResult(String result) {
        return repository.findByResult(result);
    }

    /**
     * 查询最近的日志
     */
    public List<RequestLog> findRecentLogs(String serviceName) {
        return repository.findTop100ByServiceNameOrderByTimestampDesc(serviceName);
    }

    /**
     * 统计指定时间范围内的请求数量
     */
    public long countRequests(Long startTime, Long endTime) {
        return repository.countByTimestampBetween(startTime, endTime);
    }

    /**
     * 统计指定时间范围内成功的请求数量
     */
    public long countSuccessRequests(Long startTime, Long endTime) {
        return repository.countByResultAndTimestampBetween(RequestResult.SENT_AND_RESPONDED.name(), startTime, endTime);
    }

    /**
     * 统计指定时间范围内被限流的请求数量
     */
    public long countRateLimitedRequests(Long startTime, Long endTime) {
        return repository.countByRateLimitedAndTimestampBetween(true, startTime, endTime);
    }

    /**
     * 获取所有日志总数
     */
    public long getTotalLogCount() {
        return repository.count();
    }
}
